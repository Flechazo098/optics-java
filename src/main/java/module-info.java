module com.flechazo.optics {
    requires static org.jspecify;

    requires com.google.common;
    requires io.smallrye.classfile;
    requires it.unimi.dsi.fastutil;

    exports com.flechazo.hkt;
    exports com.flechazo.hkt.function;
    exports com.flechazo.optics;
    exports com.flechazo.optics.indexed;
}
