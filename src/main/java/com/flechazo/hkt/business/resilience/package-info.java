/**
 * Defines retry, circuit-breaker, bulkhead, and compensating-saga policies for effectful
 * computations.
 *
 * <p>Policies report exhaustion, rejection, open-circuit, and compensation outcomes through their
 * declared result and exception contracts.
 */
@NullMarked
package com.flechazo.hkt.business.resilience;

import org.jspecify.annotations.NullMarked;
