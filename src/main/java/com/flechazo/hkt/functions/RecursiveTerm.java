package com.flechazo.hkt.functions;

import java.util.List;

public interface RecursiveTerm<T extends RecursiveTerm<T>> {
    int familyIndex();

    List<T> children();

    T withChildren(List<T> children);
}
