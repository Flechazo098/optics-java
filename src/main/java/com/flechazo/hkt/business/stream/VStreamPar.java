package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.effect.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class VStreamPar {
    private VStreamPar() {
    }

    private sealed interface MergeSignal<A> permits Element, SourceDone, SourceError {
    }

    private record Element<A>(A value) implements MergeSignal<A> {
    }

    private record SourceDone<A>() implements MergeSignal<A> {
    }

    private record SourceError<A>(Throwable cause) implements MergeSignal<A> {
    }

    private record IndexedResult<A>(int index, A value) {
    }

    public static <A, B> VStream<B> parEvalMap(
            VStream<A> stream,
            int concurrency,
            Function<? super A, ? extends Task<B>> mapper) {
        checkConcurrency(concurrency);
        return VStream.defer(() -> {
            ArrayList<A> batch = new ArrayList<>(concurrency);
            AtomicReference<VStream<A>> tail = new AtomicReference<>(stream);
            boolean done = pullBatch(tail, batch, concurrency);
            if (batch.isEmpty()) {
                return VStream.empty();
            }

            List<B> mapped = processOrdered(batch, mapper).unsafeRun();
            VStream<B> batchStream = VStream.fromList(mapped);
            return done ? batchStream : batchStream.concat(parEvalMap(tail.get(), concurrency, mapper));
        });
    }

    public static <A, B> VStream<B> parEvalMapUnordered(
            VStream<A> stream,
            int concurrency,
            Function<? super A, ? extends Task<B>> mapper) {
        checkConcurrency(concurrency);
        return VStream.defer(() -> {
            ArrayList<A> batch = new ArrayList<>(concurrency);
            AtomicReference<VStream<A>> tail = new AtomicReference<>(stream);
            boolean done = pullBatch(tail, batch, concurrency);
            if (batch.isEmpty()) {
                return VStream.empty();
            }

            List<B> mapped = processUnordered(batch, mapper).unsafeRun();
            VStream<B> batchStream = VStream.fromList(mapped);
            return done ? batchStream : batchStream.concat(parEvalMapUnordered(tail.get(), concurrency, mapper));
        });
    }

    public static <A, B> VStream<B> parEvalFlatMap(
            VStream<A> stream,
            int concurrency,
            Function<? super A, ? extends VStream<B>> mapper) {
        return parEvalMap(stream, concurrency, value -> Task.delay(() -> mapper.apply(value)))
                .flatMap(Function.identity());
    }

    public static <A> VStream<A> merge(VStream<A> first, VStream<A> second) {
        return merge(List.of(first, second));
    }

    public static <A> VStream<A> merge(List<VStream<A>> streams) {
        if (streams.isEmpty()) {
            return VStream.empty();
        }
        if (streams.size() == 1) {
            return streams.getFirst();
        }

        return VStream.defer(() -> {
            LinkedBlockingQueue<MergeSignal<A>> queue = new LinkedBlockingQueue<>();
            AtomicBoolean cancelled = new AtomicBoolean(false);
            ArrayList<Thread> producers = new ArrayList<>(streams.size());
            for (VStream<A> stream : streams) {
                producers.add(Thread.ofVirtual().start(() -> consumeSource(stream, queue, cancelled)));
            }
            return mergeFromQueue(queue, streams.size(), cancelled, producers);
        });
    }

    public static <A> Task<List<A>> parCollect(VStream<A> stream, int batchSize) {
        checkBatchSize(batchSize);
        return parEvalMap(stream, batchSize, Task::pure).toList();
    }

    private static <A> boolean pullBatch(AtomicReference<VStream<A>> tail, List<A> batch, int count) {
        VStream<A> current = tail.get();
        for (int i = 0; i < count; i++) {
            VStream.Step<A> step = current.pull().unsafeRun();
            switch (step) {
                case VStream.Emit<A> emit -> {
                    batch.add(emit.value());
                    current = emit.tail();
                }
                case VStream.Skip<A> skip -> {
                    current = skip.tail();
                    i--;
                }
                case VStream.Done<A> ignored -> {
                    tail.set(current);
                    return true;
                }
            }
        }
        tail.set(current);
        return false;
    }

    private static <A, B> Task<List<B>> processOrdered(
            List<A> batch,
            Function<? super A, ? extends Task<B>> mapper) {
        return Task.delay(() -> {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            try {
                CompletionService<IndexedResult<B>> completions = new ExecutorCompletionService<>(executor);
                for (int index = 0; index < batch.size(); index++) {
                    A value = batch.get(index);
                    int resultIndex = index;
                    completions.submit(() -> new IndexedResult<>(resultIndex, mapper.apply(value).asCallable().call()));
                }

                ArrayList<IndexedResult<B>> indexedResults = new ArrayList<>(batch.size());
                for (int i = 0; i < batch.size(); i++) {
                    indexedResults.add(getFuture(completions.take()));
                }

                indexedResults.sort(Comparator.comparingInt(IndexedResult::index));
                ArrayList<B> results = new ArrayList<>(indexedResults.size());
                for (IndexedResult<B> result : indexedResults) {
                    results.add(result.value());
                }
                return results;
            } finally {
                executor.shutdownNow();
            }
        });
    }

    private static <A, B> Task<List<B>> processUnordered(
            List<A> batch,
            Function<? super A, ? extends Task<B>> mapper) {
        return Task.delay(() -> {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            try {
                CompletionService<B> completions = new ExecutorCompletionService<>(executor);
                for (A value : batch) {
                    completions.submit(mapper.apply(value).asCallable());
                }

                ArrayList<B> results = new ArrayList<>(batch.size());
                for (int i = 0; i < batch.size(); i++) {
                    results.add(getFuture(completions.take()));
                }
                return results;
            } finally {
                executor.shutdownNow();
            }
        });
    }

    private static <A> void consumeSource(
            VStream<A> source,
            LinkedBlockingQueue<MergeSignal<A>> queue,
            AtomicBoolean cancelled) {
        VStream<A> current = source;
        try {
            while (!cancelled.get()) {
                VStream.Step<A> step = current.pull().unsafeRun();
                switch (step) {
                    case VStream.Emit<A> emit -> {
                        queue.put(new Element<>(emit.value()));
                        current = emit.tail();
                    }
                    case VStream.Skip<A> skip -> current = skip.tail();
                    case VStream.Done<A> ignored -> {
                        queue.put(new SourceDone<>());
                        return;
                    }
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Throwable error) {
            if (!cancelled.getAndSet(true)) {
                queue.offer(new SourceError<>(error));
            }
        } finally {
            current.close().unsafeRun();
        }
    }

    private static <A> VStream<A> mergeFromQueue(
            LinkedBlockingQueue<MergeSignal<A>> queue,
            int remainingSources,
            AtomicBoolean cancelled,
            List<Thread> producers) {
        return new VStream<>() {
            @Override
            public Task<Step<A>> pull() {
                return Task.delay(() -> {
                    try {
                        MergeSignal<A> signal = queue.take();
                        return switch (signal) {
                            case Element<A> element ->
                                    new Emit<>(element.value(), mergeFromQueue(queue, remainingSources, cancelled, producers));
                            case SourceDone<A> ignored -> {
                                int remaining = remainingSources - 1;
                                yield remaining <= 0
                                        ? new Done<>()
                                        : new Skip<>(mergeFromQueue(queue, remaining, cancelled, producers));
                            }
                            case SourceError<A> error -> throw rethrow(error.cause());
                        };
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Merge interrupted", interrupted);
                    }
                });
            }

            @Override
            public Task<Unit> close() {
                return Task.delay(() -> {
                    cancelled.set(true);
                    queue.clear();
                    for (Thread producer : producers) {
                        producer.interrupt();
                    }
                    for (Thread producer : producers) {
                        try {
                            producer.join();
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    return Unit.INSTANCE;
                });
            }
        };
    }

    private static <A> A getFuture(Future<A> future) {
        try {
            return future.get();
        } catch (ExecutionException exception) {
            throw unwrap(exception);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(interrupted);
        }
    }

    private static RuntimeException rethrow(Throwable cause) {
        Throwable unwrapped = unwrap(cause);
        if (unwrapped instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(unwrapped);
    }

    private static RuntimeException unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof ExecutionException || current instanceof CompletionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        if (current instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (current instanceof Error fatal) {
            throw fatal;
        }
        return new RuntimeException(current);
    }

    private static void checkConcurrency(int concurrency) {
        if (concurrency <= 0) {
            throw new IllegalArgumentException("concurrency must be positive");
        }
    }

    private static void checkBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
    }
}
