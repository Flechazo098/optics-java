package com.flechazo.hkt;

import com.flechazo.hkt.business.control.ListK;
import java.util.List;

public final class ImplicitShow {

// given —— 声明隐式实例。
// for —— 指定适用的具体类型。
// provides —— 指定实现的类型类接口。
// using —— 指定获取实例的方法引用。
// unbox —— 指定从盒子解包的具体方法。
given maybeFunctor<A> {
    for Maybe<A>;
    provides Functor<Maybe.Mu, Maybe.MaybeApplicative.MuProof>;
    using Maybe.applicative();
    unbox Maybe.unbox;
}

given listKFunctor<A> {
    for ListK<A>;
    provides Functor<ListK.Mu, ListK.Instance.Mu>;
    using ListK.applicative();
    unbox ListK.unbox;
}

given identityFunctor<A> {
    for IdF<A>;
    provides Functor<IdF.Mu, IdF.InstanceMu>;
    using IdF.applicative();
    unbox IdF.unbox;
}

// using —— 方法参数中声明隐式参数，自动匹配 given 提供的实例。
public static <F extends K1, P extends Functor.Mu, A> App<F, String> show(
        App<F, A> value
) using (
        Functor<F, P> functor
) {
    return functor.map(element -> "got: " + element, value);
}

public static void main(String[] args) {
    // Maybe.some : Some[value=...]
    Maybe<String> maybeResult = ImplicitShow.show(Maybe.some("apple"));
    System.out.println("maybe: " + maybeResult);

    // ListK.toList : [..., ..., ...]
    ListK<String> listResult = ImplicitShow.show(ListK.of(List.of("apple", "pear", "peach")));
    System.out.println("listK: " + listResult.toList());

    // IdF.value : ...
    IdF<String> idResult = ImplicitShow.show(IdF.of("hello"));
    System.out.println("idF: " + idResult.value());
}
}
