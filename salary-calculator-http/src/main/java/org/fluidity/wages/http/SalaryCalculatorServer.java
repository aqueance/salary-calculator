package org.fluidity.wages.http;

import org.fluidity.composition.Component;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.deployment.cli.Application;
import org.fluidity.foundation.Log;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

/**
 * An HTTP server that calculates salaries from time sheets, and exposes its own HTML client at "/".
 */
@Component
@SuppressWarnings("WeakerAccess")
final class SalaryCalculatorServer implements Application {

    static final int DEFAULT_PORT = 8080;

    private final ContainerTermination stopping;
    private final Log log;

    private final Vertx vertx;

    private final ResourceHandler resources;
    private final SalaryCalculatorHandler calculator;

    SalaryCalculatorServer(final VertxInstance vertx,
                           final SalaryCalculatorHandler calculator,
                           final ResourceHandler resources,
                           final ContainerTermination stopping,
                           final Log<SalaryCalculatorServer> log) {
        this.vertx = vertx.get();
        this.resources = resources;
        this.calculator = calculator;
        this.stopping = stopping;
        this.log = log;
    }

    public void run(final String... arguments) {
        final HttpServer server = vertx.createHttpServer();
        final Router router = Router.router(vertx);

        // Computes salary from an uploaded CSV file.
        router.post("/calculate").produces("application/json").handler(calculator);

        // Serves the static files from src/main/resources/webroot that comprise the HTML client.
        router.get().handler(resources);

        // Prevents HTML body to be sent back by Vert.x on errors (Mithril chokes on that).
        router.route().handler(context -> {
            final HttpServerResponse response = context.response();

            response.setStatusCode(404);
            response.end();
        });

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
