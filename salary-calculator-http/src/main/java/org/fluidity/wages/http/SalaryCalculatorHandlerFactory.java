package org.fluidity.wages.http;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Function;

import org.fluidity.composition.Component;
import org.fluidity.wages.SalaryDetails;
import org.fluidity.wages.csv.SalaryCalculator;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * A Vert.x web route handler factory for a CSV upload and salary processing thereof.
 * <p>
 * The salary calculation is blocking, so this factory takes a {@link Vertx}
 * instance and returns the actual route handler that uses it to invoke blocking operations.
 */
@Component(api = SalaryCalculatorHandlerFactory.class)
final class SalaryCalculatorHandlerFactory implements Function<Vertx, Handler<RoutingContext>> {

    private final SalaryCalculator calculator;

    SalaryCalculatorHandlerFactory(final SalaryCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public Handler<RoutingContext> apply(final Vertx vertx) {
        return context -> {
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

                    final Handler<Future<JsonObject>> logic = future -> {
                        final JsonObject json = new JsonObject();
                        json.put("currency", "USD");

                        final JsonArray months = new JsonArray();
                        json.put("months", months);

                        final Consumer<SalaryDetails> printer = new Consumer<SalaryDetails>() {

                            // The current month.
                            private LocalDate month;

                            // The array of people objects for the current month.
                            private JsonArray peopleArray = new JsonArray();

                            @Override
                            public void accept(final SalaryDetails details) {
                                if (month == null || !details.month.equals(month)) {
                                    month = details.month;

                                    final JsonObject monthObject = new JsonObject();
                                    months.add(monthObject);

                                    monthObject.put("year", month.getYear());
                                    monthObject.put("month", month.getMonthValue());

                                    monthObject.put("people", peopleArray = new JsonArray());
                                }

                                final JsonObject peopleObject = new JsonObject();
                                peopleArray.add(peopleObject);

                                peopleObject.put("id", details.personId);
                                peopleObject.put("name", details.personName);
                                peopleObject.put("salary", details.amount());
                            }
                        };

                        // The actual business logic.

                        try {
                            calculator.process(new InputStreamReader(new ByteArrayInputStream(buffer.getBytes()), encoding), printer);
                        } catch (final Exception error) {
                            json.clear();
                            json.put("error", error.getMessage());
                        }

                        future.complete(json);
                    };

                    vertx.executeBlocking(logic, result -> {
                        response.putHeader("content-type", context.getAcceptableContentType() + "; charset=utf-8");
                        response.end(result.result().encode());
                    });
                });

                upload.handler(buffer::appendBuffer);
            });
        };
    }
}
