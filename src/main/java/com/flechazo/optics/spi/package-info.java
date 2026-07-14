/**
 * Defines service-provider contracts used to obtain caller-authorized method-handle lookups for
 * generated optic executors.
 *
 * <p>Providers return lookups only for modules and packages for which they are authorized to
 * define or access generated classes.
 */
@NullMarked
package com.flechazo.optics.spi;

import org.jspecify.annotations.NullMarked;
