package com.flechazo.hkt.functions;

import com.flechazo.hkt.Maybe;

import java.util.Objects;

@FunctionalInterface
public interface PointFreeRule {
    <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression);

    default <A> PointFree<A> rewriteOrSame(PointFree<A> expression) {
        return rewrite(expression).orElse(expression);
    }

    static PointFreeRule nop() {
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return Maybe.none();
            }
        };
    }

    static PointFreeRule seq(PointFreeRule... rules) {
        Objects.requireNonNull(rules, "rules");
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                PointFree<A> current = expression;
                boolean rewritten = false;
                for (PointFreeRule rule : rules) {
                    Objects.requireNonNull(rule, "rule");
                    Maybe<PointFree<A>> next = rule.rewrite(current);
                    if (next.isDefined()) {
                        current = next.get();
                        rewritten = true;
                    }
                }
                return rewritten ? Maybe.some(current) : Maybe.none();
            }
        };
    }

    static PointFreeRule choice(PointFreeRule... rules) {
        Objects.requireNonNull(rules, "rules");
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                for (PointFreeRule rule : rules) {
                    Objects.requireNonNull(rule, "rule");
                    Maybe<PointFree<A>> next = rule.rewrite(expression);
                    if (next.isDefined()) {
                        return next;
                    }
                }
                return Maybe.none();
            }
        };
    }

    static PointFreeRule all(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return expression.all(rule);
            }
        };
    }

    static PointFreeRule one(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                return expression.one(rule);
            }
        };
    }

    static PointFreeRule once(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                Maybe<PointFree<A>> current = rule.rewrite(expression);
                return current.isDefined() ? current : expression.one(this);
            }
        };
    }

    static PointFreeRule many(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                PointFree<A> current = expression;
                boolean rewritten = false;
                while (true) {
                    Maybe<PointFree<A>> next = rule.rewrite(current);
                    if (next.isEmpty() || Objects.equals(next.get(), current)) {
                        return rewritten ? Maybe.some(current) : Maybe.none();
                    }
                    current = next.get();
                    rewritten = true;
                }
            }
        };
    }

    static PointFreeRule topDown(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                Maybe<PointFree<A>> current = rule.rewrite(expression);
                if (current.isDefined()) {
                    return current;
                }
                return expression.all(this);
            }
        };
    }

    static PointFreeRule bottomUp(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                PointFree<A> current = expression;
                boolean rewritten = false;

                Maybe<PointFree<A>> children = expression.all(this);
                if (children.isDefined()) {
                    current = children.get();
                    rewritten = true;
                }

                Maybe<PointFree<A>> local = rule.rewrite(current);
                if (local.isDefined()) {
                    return local;
                }
                return rewritten ? Maybe.some(current) : Maybe.none();
            }
        };
    }

    static PointFreeRule everywhere(PointFreeRule topDown, PointFreeRule bottomUp) {
        Objects.requireNonNull(topDown, "topDown");
        Objects.requireNonNull(bottomUp, "bottomUp");
        return new PointFreeRule() {
            @Override
            public <A> Maybe<PointFree<A>> rewrite(PointFree<A> expression) {
                PointFree<A> current = expression;
                boolean rewritten = false;

                Maybe<PointFree<A>> top = topDown.rewrite(current);
                if (top.isDefined()) {
                    current = top.get();
                    rewritten = true;
                }

                Maybe<PointFree<A>> children = all(this).rewrite(current);
                if (children.isDefined()) {
                    current = children.get();
                    rewritten = true;
                }

                Maybe<PointFree<A>> bottom = bottomUp.rewrite(current);
                if (bottom.isDefined()) {
                    current = bottom.get();
                    rewritten = true;
                }

                return rewritten ? Maybe.some(current) : Maybe.none();
            }
        };
    }

}
