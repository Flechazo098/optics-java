package com.flechazo.hkt.business.stream;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Monoid;
import com.flechazo.hkt.Traversable;
import com.flechazo.hkt.business.control.ListK;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public enum VStreamTraverse implements Traversable<VStream.Mu, VStream.InstanceMu> {
    INSTANCE;

    @Override
    public <A, B> App<VStream.Mu, B> map(Function<? super A, ? extends B> f, App<VStream.Mu, A> fa) {
        return VStreamFunctor.INSTANCE.map(f, fa);
    }

    @Override
    public <F extends K1, A, B> App<F, App<VStream.Mu, B>> traverse(
            Applicative<F, ?> applicative,
            Function<? super A, ? extends App<F, B>> f,
            App<VStream.Mu, A> value) {
        Objects.requireNonNull(applicative, "applicative");
        Objects.requireNonNull(f, "f");
        List<A> elements = VStream.unbox(value).toList().unsafeRun();
        App<F, ListK<B>> result = applicative.of(ListK.empty());
        for (A element : elements) {
            App<F, B> mapped = Objects.requireNonNull(f.apply(element), "traverse result");
            result = applicative.map2(result, mapped, ListK::append);
        }
        return applicative.map(list -> VStream.fromList(list.toList()), result);
    }

    @Override
    public <A, M> M foldMap(Monoid<M> monoid, Function<? super A, ? extends M> f, App<VStream.Mu, A> value) {
        Objects.requireNonNull(monoid, "monoid");
        Objects.requireNonNull(f, "f");
        return VStream.unbox(value)
                .foldLeft(monoid.empty(), (acc, element) -> monoid.combine(acc, f.apply(element)))
                .unsafeRun();
    }

}
