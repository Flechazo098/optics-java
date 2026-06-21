package com.flechazo.hkt;

public interface App2<F extends K2, A, B> extends App<App2.Mu<F, A>, B> {
    final class Mu<F extends K2, A> implements K1 {
        private Mu() {
        }
    }
}
