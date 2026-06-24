package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.optics.Traversal;

import java.util.function.Function;

public final class StringTraversals {
    private StringTraversals() {
    }

    public static Traversal<String, Character> characters() {
        return new Traversal<>() {
            @Override
            public <F extends K1> App<F, String> modifyF(
                    Function<Character, App<F, Character>> f, String source, Applicative<F, ?> applicative) {
                App<F, StringBuilder> acc = applicative.of(new StringBuilder(source.length()));
                for (int i = 0; i < source.length(); i++) {
                    char ch = source.charAt(i);
                    acc =
                            applicative.map2(
                                    acc,
                                    f.apply(ch),
                                    (builder, next) -> {
                                        StringBuilder copy = new StringBuilder(builder);
                                        copy.append(next);
                                        return copy;
                                    });
                }
                return applicative.map(StringBuilder::toString, acc);
            }
        };
    }
}
