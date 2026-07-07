module com.flechazo.optics {
    requires static org.jspecify;

    requires com.google.common;
    requires it.unimi.dsi.fastutil;

    exports com.flechazo.hkt;
    exports com.flechazo.hkt.business.capability;
    exports com.flechazo.hkt.business.control;
    exports com.flechazo.hkt.business.context;
    exports com.flechazo.hkt.business.core;
    exports com.flechazo.hkt.business.data;
    exports com.flechazo.hkt.business.effect;
    exports com.flechazo.hkt.business.stream;
    exports com.flechazo.hkt.function;
    exports com.flechazo.hkt.functions;
    exports com.flechazo.hkt.type;
    exports com.flechazo.optics;
    exports com.flechazo.optics.focus;
    exports com.flechazo.optics.generated;
    exports com.flechazo.optics.indexed;
    exports com.flechazo.optics.util;
    exports com.flechazo.hkt.business.util;
}
