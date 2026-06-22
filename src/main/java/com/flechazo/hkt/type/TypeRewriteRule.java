package com.flechazo.hkt.type;

import com.flechazo.hkt.Maybe;

import java.util.Objects;

@FunctionalInterface
public interface TypeRewriteRule {
    Maybe<TypeExpr> rewrite(TypeExpr expression);

    default TypeExpr rewriteOrSame(TypeExpr expression) {
        Objects.requireNonNull(expression, "expression");
        return rewrite(expression).orElse(expression);
    }

    static TypeRewriteRule choice(TypeRewriteRule first, TypeRewriteRule second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return expression -> first.rewrite(expression).or(() -> second.rewrite(expression));
    }

    static TypeRewriteRule seq(TypeRewriteRule first, TypeRewriteRule second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return expression -> first.rewrite(expression).flatMap(second::rewrite);
    }

    static TypeRewriteRule all(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return expression -> expression.all(rule);
    }

    static TypeRewriteRule one(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return expression -> expression.one(rule);
    }

    static TypeRewriteRule topDown(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return expression -> rule.rewrite(expression).or(() -> expression.all(topDown(rule)));
    }

    static TypeRewriteRule bottomUp(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return expression -> {
            TypeExpr rewrittenChildren = expression.all(bottomUp(rule)).orElse(expression);
            return rule.rewrite(rewrittenChildren)
                    .or(() -> rewrittenChildren == expression || rewrittenChildren.equals(expression)
                            ? Maybe.none()
                            : Maybe.some(rewrittenChildren));
        };
    }

    static TypeRewriteRule once(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return expression -> rule.rewrite(expression).or(() -> expression.one(once(rule)));
    }

    static TypeRewriteRule many(TypeRewriteRule rule) {
        Objects.requireNonNull(rule, "rule");
        return expression -> {
            TypeExpr current = expression;
            boolean changed = false;
            while (true) {
                Maybe<TypeExpr> next = rule.rewrite(current);
                if (next.isEmpty()) {
                    return changed ? Maybe.some(current) : Maybe.none();
                }
                current = next.get();
                changed = true;
            }
        };
    }
}
