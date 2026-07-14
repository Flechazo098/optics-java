package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.hkt.internal.AccumulationBuffer;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.function.Function;

public final class StringTraversals {
    private StringTraversals() {
    }

    public static Traversal<String, Character> characters() {
        PTraversal<String, String, Character, Character> typed = OpticMetadata.optic(new PTraversal<>() {
            @Override
            public <F extends K1> App<F, String> modifyF(
                    Function<Character, App<F, Character>> f, String source, Applicative<F, ?> applicative) {
                App<F, AccumulationBuffer<Character>> acc = applicative.of(AccumulationBuffer.empty());
                for (int i = 0; i < source.length(); i++) {
                    char ch = source.charAt(i);
                    acc = applicative.map2(acc, f.apply(ch), AccumulationBuffer::prepend);
                }
                return applicative.map(values -> {
                    StringBuilder result = new StringBuilder(source.length());
                    values.toList().forEach(result::append);
                    return result.toString();
                }, acc);
            }
        }, Maybe.some(PointFreeOptic.stringCharacters()));
        return Traversal.from(OpticPrograms.traversal(
                typed,
                OpticPrograms.structured("stringCharactersTraversal", null)));
    }
}
