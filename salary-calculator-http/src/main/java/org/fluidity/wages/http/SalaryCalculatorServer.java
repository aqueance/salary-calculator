package org.fluidity.wages.http;

import java.util.Arrays;

import org.fluidity.composition.Component;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.deployment.cli.Application;
import org.fluidity.foundation.Log;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * TODO
 */
@Component
@SuppressWarnings("WeakerAccess")
final class SalaryCalculatorServer implements Application {

    static final int DEFAULT_PORT = 8080;

    private final SalaryCalculatorHandlerFactory factory;
    private final ContainerTermination stopping;
    private final Log log;

    SalaryCalculatorServer(final SalaryCalculatorHandlerFactory factory, final ContainerTermination stopping, final Log<SalaryCalculatorServer> log) {
        this.factory = factory;
        this.stopping = stopping;
        this.log = log;
    }

    public void run(final String... arguments) {
        final Vertx vertx = Vertx.vertx();

        final HttpServer server = vertx.createHttpServer();
        final Router router = Router.router(vertx);

        // Computes salary from an uploaded CSV file.
        router.post("/calculate").produces("application/json").handler(factory.apply(vertx));

        // Serves the static files from src/main/resources/webroot that comprise the HTML client.
        router.get().handler(StaticHandler.create());

        try {
            final String host = "localhost";        // hard-coded for now, as this is only a demo
            final int port = arguments.length > 0 ? Integer.parseInt(arguments[0]) : DEFAULT_PORT;

            server.requestHandler(router::accept).listen(port, host);

            log.info("Server listening on http://%s:%d", host, port);
            stopping.add(() -> {
                vertx.close();
                log.info("Server stopped");
            });
        } catch (final NumberFormatException error) {
            log.error("supplied port '%s' is not a number", arguments[0]);
        }
    }
}
