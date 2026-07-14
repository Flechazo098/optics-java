/**
 * Defines monomorphic and polymorphic optics for immutable data access, update, traversal, and
 * querying.
 *
 * <p>Writable optics return rebuilt source values and do not mutate their inputs; query optics
 * expose observations without representing a write capability.
 */
@NullMarked
package com.flechazo.optics;

import org.jspecify.annotations.NullMarked;
