package com.flechazo.hkt;

@FunctionalInterface
public interface Natural<F extends K1, G extends K1> {
    <A> App<G, A> apply(App<F, A> value);

    default <H extends K1> Natural<F, H> andThen(Natural<G, H> next) {
        return new Natural<>() {
            @Override
            public <A> App<H, A> apply(App<F, A> value) {
                return next.apply(Natural.this.apply(value));
            }
        };
    }

    default <E extends K1> Natural<E, G> compose(Natural<E, F> before) {
        return before.andThen(this);
    }

    @SuppressWarnings("unchecked")
    static <F extends K1> Natural<F, F> identity() {
        return (Natural<F, F>) Identity.INSTANCE;
    }

    enum Identity implements Natural<K1, K1> {
        INSTANCE;

        @Override
        public <A> App<K1, A> apply(App<K1, A> value) {
            return value;
        }
    }
}
