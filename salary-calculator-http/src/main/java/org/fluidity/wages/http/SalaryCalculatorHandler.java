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

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.time.LocalDate;
import java.util.function.Consumer;

import org.fluidity.composition.Component;
import org.fluidity.wages.SalaryDetails;
import org.fluidity.wages.csv.SalaryCalculator;
import org.fluidity.wages.http.json.JsonOutput;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * A Vert.x web route handler factory for a CSV upload and salary processing thereof.
 * <p>
 * The salary calculation is blocking, so this factory takes a {@link Vertx}
 * instance and returns the actual route handler that uses it to invoke blocking operations.
 */
@Component(api = SalaryCalculatorHandler.class)
final class SalaryCalculatorHandler implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final SalaryCalculator calculator;

    SalaryCalculatorHandler(final VertxInstance vertx, final SalaryCalculator calculator) {
        this.vertx = vertx.get();
        this.calculator = calculator;
    }

    @Override
    public void handle(final RoutingContext context) {
        final HttpServerRequest request = context.request();
        final HttpServerResponse response = context.response();

        request.setExpectMultipart(true);
        request.uploadHandler(upload -> {
            final Buffer buffer = Buffer.buffer();

            upload.exceptionHandler(error -> response.end("upload failed: " + error.getMessage()));

            upload.endHandler(ignored -> {
                final Charset encoding;

                try {
                    final String charset = upload.charset();
                    encoding = charset != null ? Charset.forName(charset) : StandardCharsets.UTF_8;
                } catch (final UnsupportedCharsetException error) {
                    throw new IllegalArgumentException("unknown encoding: " + upload.charset());
                }

                response.putHeader("Content-Type", context.getAcceptableContentType() + "; charset=utf-8");
                response.setChunked(true);

                final Handler<Future<Void>> logic = future -> {
                    final JsonOutput.Object.Root json = JsonOutput.object(16384, response::write);
                    final JsonOutput.Array months = json.array("months");

                    final Consumer<SalaryDetails> printer = new Consumer<SalaryDetails>() {

                        // The current month.
                        private LocalDate month;

                        // The array of people objects for the current month.
                        private JsonOutput.Array peopleArray;

                        @Override
                        public void accept(final SalaryDetails details) {
                            if (month == null || !details.month.equals(month)) {
                                month = details.month;

                                final JsonOutput.Object monthObject = months.object();

                                monthObject.add("year", month.getYear());
                                monthObject.add("month", month.getMonthValue());

                                peopleArray = monthObject.array("people");
                            }

                            final JsonOutput.Object peopleObject = peopleArray.object();

                            peopleObject.add("id", details.personId);
                            peopleObject.add("name", details.personName);
                            peopleObject.add("salary", details.amount());
                        }
                    };

                    // The actual business logic.

                    try {
                        calculator.process(new InputStreamReader(new ByteArrayInputStream(buffer.getBytes()), encoding), printer);
                    } catch (final Exception error) {
                        json.add("error", error.getMessage());
                    } finally {
                        json.close(response::end);
                    }

                    future.complete();
                };

                vertx.executeBlocking(logic, null);
            });

            upload.handler(buffer::appendBuffer);
        });
    }
}
