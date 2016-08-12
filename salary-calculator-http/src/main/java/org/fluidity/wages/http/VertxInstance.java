package org.fluidity.wages.http;

import java.util.function.Supplier;

import org.fluidity.composition.Component;

import io.vertx.core.Vertx;

/**
 * Encapsulates the Vert.x instance used by this server.
 */
@Component(api = VertxInstance.class)
final class VertxInstance implements Supplier<Vertx> {

    private final Vertx vertx = Vertx.vertx();

    @Override
    public Vertx get() {
        return vertx;
    }
}
