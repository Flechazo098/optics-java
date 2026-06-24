package com.flechazo.hkt.functions;

import java.util.Objects;

@FunctionalInterface
public interface PointFreeRule {
    <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression);

    default <A> PointFree<A> rewriteOrSame(RewriteContext context, PointFree<A> expression) {
        RewriteResult<A> result = rewrite(context, expression);
        return result.changed() ? result.expression() : expression;
    }

    static PointFreeRule nop() {
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                return RewriteResult.unchanged(expression);
            }
        };
    }

    static PointFreeRule seq(PointFreeRule... rules) {
        Objects.requireNonNull(rules, "rules");
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                PointFree<A> current = expression;
                boolean changed = false;
                for (PointFreeRule rule : rules) {
                    Objects.requireNonNull(rule, "rule");
                    RewriteResult<A> next = rule.rewrite(context, current);
                    if (next.changed()) {
                        current = next.expression();
                        changed = true;
                    }
                }
                return changed ? RewriteResult.changed(current) : RewriteResult.unchanged(expression);
            }
        };
    }

    static PointFreeRule choice(PointFreeRule... rules) {
        Objects.requireNonNull(rules, "rules");
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                for (PointFreeRule rule : rules) {
                    Objects.requireNonNull(rule, "rule");
                    RewriteResult<A> next = rule.rewrite(context, expression);
                    if (next.changed()) {
                        return next;
                    }
                }
                return RewriteResult.unchanged(expression);
            }
        };
    }

    static PointFreeRule all(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                return expression.all(context, rule);
            }
        };
    }

    static PointFreeRule one(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                return expression.one(context, rule);
            }
        };
    }

    static PointFreeRule once(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                RewriteResult<A> current = rule.rewrite(context, expression);
                return current.changed() ? current : expression.one(context, this);
            }
        };
    }

    static PointFreeRule many(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                PointFree<A> current = expression;
                boolean changed = false;
                while (true) {
                    RewriteResult<A> next = rule.rewrite(context, current);
                    if (!next.changed() || Objects.equals(next.expression(), current)) {
                        return changed ? RewriteResult.changed(current) : RewriteResult.unchanged(expression);
                    }
                    current = next.expression();
                    changed = true;
                }
            }
        };
    }

    static PointFreeRule topDown(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                RewriteResult<A> current = rule.rewrite(context, expression);
                return current.changed() ? current : expression.all(context, this);
            }
        };
    }

    static PointFreeRule bottomUp(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                PointFree<A> current = expression;
                boolean changed = false;

                RewriteResult<A> children = expression.all(context, this);
                if (children.changed()) {
                    current = children.expression();
                    changed = true;
                }

                RewriteResult<A> local = rule.rewrite(context, current);
                if (local.changed()) {
                    return local;
                }
                return changed ? RewriteResult.changed(current) : RewriteResult.unchanged(expression);
            }
        };
    }

    static PointFreeRule bottomUpFix(PointFreeRule rule) {
        Objects.requireNonNull(rule, "rule");
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                PointFree<A> current = expression;
                boolean changed = false;

                while (true) {
                    RewriteResult<A> children = current.all(context, this);
                    if (children.changed()) {
                        current = children.expression();
                        changed = true;
                    }

                    RewriteResult<A> local = rule.rewrite(context, current);
                    if (local.changed()) {
                        current = local.expression();
                        changed = true;
                        continue;
                    }

                    return changed ? RewriteResult.changed(current) : RewriteResult.unchanged(expression);
                }
            }
        };
    }

    static PointFreeRule everywhere(PointFreeRule topDown, PointFreeRule bottomUp) {
        Objects.requireNonNull(topDown, "topDown");
        Objects.requireNonNull(bottomUp, "bottomUp");
        return new PointFreeRule() {
            @Override
            public <A> RewriteResult<A> rewrite(RewriteContext context, PointFree<A> expression) {
                PointFree<A> current = expression;
                boolean changed = false;

                RewriteResult<A> top = topDown.rewrite(context, current);
                if (top.changed()) {
                    current = top.expression();
                    changed = true;
                }

                RewriteResult<A> children = all(this).rewrite(context, current);
                if (children.changed()) {
                    current = children.expression();
                    changed = true;
                }

                RewriteResult<A> bottom = bottomUp.rewrite(context, current);
                if (bottom.changed()) {
                    current = bottom.expression();
                    changed = true;
                }

                return changed ? RewriteResult.changed(current) : RewriteResult.unchanged(expression);
            }
        };
    }
}
