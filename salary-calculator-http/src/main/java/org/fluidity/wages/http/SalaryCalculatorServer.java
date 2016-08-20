/*
 * Copyright (c) 2016 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    private final Log log;
    private final Vertx vertx;
    private final ResourceHandler resources;
    private final SalaryCalculatorHandler calculator;

    SalaryCalculatorServer(final VertxInstance vertx,
                           final ResourceHandler resources,
                           final SalaryCalculatorHandler calculator,
                           final ContainerTermination stopping,
                           final Log<SalaryCalculatorServer> log) {
        this.vertx = vertx.get();
        this.resources = resources;
        this.calculator = calculator;
        this.log = log;

        stopping.add(() -> {
            this.vertx.close();
            log.info("Server stopped");
        });
    }

    public void run(final String... arguments) {
        final int port;

        try {
            port = arguments.length > 0 ? Integer.parseInt(arguments[0]) : DEFAULT_PORT;
        } catch (final NumberFormatException error) {
            throw new IllegalArgumentException(String.format("Expected a number: %s", arguments[0]));
        }

        final Router router = router(vertx);
        final HttpServer server = server(vertx);

        server.requestHandler(router::accept).listen(port);

        log.info("Server listening on port %d", port);
    }

    private HttpServer server(final Vertx vertx) {
        return vertx.createHttpServer();
    }

    private Router router(final Vertx vertx) {
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

        return router;
    }
}
