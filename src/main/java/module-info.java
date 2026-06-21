module com.flechazo.optics {
    requires static org.jspecify;

    requires io.smallrye.classfile;

    exports com.flechazo.hkt;
    exports com.flechazo.hkt.function;
    exports com.flechazo.optics;
    exports com.flechazo.optics.focus;
    exports com.flechazo.optics.generated;
    exports com.flechazo.optics.indexed;
    exports com.flechazo.optics.util;
}
