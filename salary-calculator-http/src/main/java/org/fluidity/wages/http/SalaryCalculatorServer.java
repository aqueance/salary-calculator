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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fluidity.composition.Component;
import org.fluidity.deployment.cli.Application;
import org.fluidity.foundation.Log;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * An HTTP server that calculates salaries from time sheets, and exposes its own HTML client at "/".
 */
@Component
@SuppressWarnings("WeakerAccess")
final class SalaryCalculatorServer implements Application {

    static final int DEFAULT_PORT = 8080;
    static final String CALCULATOR_URI = "/calculate";

    private final Log log;

    // Computes salary from an uploaded CSV file.
    private final SalaryCalculatorHandler calculator;

    // Serves the static files from src/main/resources/webroot that comprise the HTML client.
    private final ResourceHandler resources;

    SalaryCalculatorServer(final ResourceHandler resources, final SalaryCalculatorHandler calculator, final Log<SalaryCalculatorServer> log) {
        this.resources = resources;
        this.calculator = calculator;
        this.log = log;
    }

    public void run(final String... arguments) throws Exception {
        final int port;

        try {
            port = arguments.length > 0 ? Integer.parseInt(arguments[0]) : DEFAULT_PORT;
        } catch (final NumberFormatException error) {
            throw new IllegalArgumentException(String.format("Expected a number: %s", arguments[0]));
        }

        final Server server = new Server(port);

        server.setStopAtShutdown(true);
        server.setStopTimeout(TimeUnit.SECONDS.toMillis(1));

        server.setHandler(router());

        server.start();
        server.join();

        log.info("Server listening on port %d", server.getURI());
    }

    private Handler router() {
        return new AbstractHandler() {
            @Override
            public void handle(final String uri,
                               final Request control,
                               final HttpServletRequest request,
                               final HttpServletResponse response) throws IOException, ServletException {
                final Handler handler = uri.equals(CALCULATOR_URI) ? calculator : resources;
                handler.handle(uri, control, request, response);
                control.setHandled(true);
            }
        };
    }
}
