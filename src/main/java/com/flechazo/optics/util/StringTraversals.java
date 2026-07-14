package com.flechazo.optics.util;

import com.flechazo.hkt.App;
import com.flechazo.hkt.Applicative;
import com.flechazo.hkt.K1;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.business.data.Chain;
import com.flechazo.hkt.functions.PointFreeOptic;
import com.flechazo.optics.PTraversal;
import com.flechazo.optics.Traversal;
import com.flechazo.optics.internal.OpticMetadata;
import com.flechazo.optics.internal.OpticPrograms;

import java.util.function.Function;

/**
 * Provides traversals over strings.
 */
public final class StringTraversals {
    private StringTraversals() {
    }

    /**
     * Creates a traversal that focuses the characters of a string in encounter order.
     *
     * @return a traversal over every UTF-16 code unit represented as a character
     */
    public static Traversal<String, Character> characters() {
        PTraversal<String, String, Character, Character> typed = OpticMetadata.optic(new PTraversal<>() {
            @Override
            public <F extends K1> App<F, String> modifyF(
                    Function<Character, App<F, Character>> f, String source, Applicative<F, ?> applicative) {
                App<F, Chain<Character>> acc = applicative.of(Chain.empty());
                for (int i = 0; i < source.length(); i++) {
                    char ch = source.charAt(i);
                    acc = applicative.map2(acc, f.apply(ch), Chain::append);
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
