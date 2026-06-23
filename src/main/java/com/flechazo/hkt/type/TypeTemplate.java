package com.flechazo.hkt.type;

public interface TypeTemplate {
    int size();

    TypeFamily apply(TypeFamily family);
}
