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
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.time.LocalDate;
import java.util.function.Consumer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fluidity.composition.Component;
import org.fluidity.wages.SalaryDetails;
import org.fluidity.wages.csv.SalaryCalculator;
import org.fluidity.wages.http.json.JsonOutput;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * A Jetty handler for a CSV upload and salary processing thereof.
 */
@Component(api = SalaryCalculatorHandler.class)
final class SalaryCalculatorHandler extends AbstractHandler {

    private static final String CHARSET_PARAM = "charset=";
    private static final int CHARSET_PARAM_LENGTH = CHARSET_PARAM.length();

    private final SalaryCalculator calculator;

    SalaryCalculatorHandler(final SalaryCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public void handle(final String uri, final Request control, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        final HttpMethod method = HttpMethod.fromString(request.getMethod());

        if (method != HttpMethod.POST) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.addHeader("Allow", "POST");
        } else {
            try {
                final ServletFileUpload upload = new ServletFileUpload();

                final FileItemIterator files = upload.getItemIterator(request);

                if (!files.hasNext()) {
                    throw new FileUploadException("no file uploaded");
                } else {
                    final FileItemStream file = files.next();

                    final String contentType = file.getContentType();
                    final int cs = contentType.indexOf(CHARSET_PARAM);
                    final String charset = cs < 0 ? null : contentType.substring(cs + CHARSET_PARAM_LENGTH);

                    final Charset encoding;

                    try {
                        encoding = charset != null ? Charset.forName(charset) : StandardCharsets.UTF_8;
                        upload.setHeaderEncoding(encoding.name());
                    } catch (final UnsupportedCharsetException error) {
                        throw new IllegalArgumentException("unknown encoding: " + charset);
                    }

                    response.setContentType("application/json; charset=utf-8");

                    final PrintWriter output = response.getWriter();

                    final JsonOutput.Object.Root json = JsonOutput.object(16384, output::write);
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
                        calculator.process(new InputStreamReader(file.openStream(), encoding), printer);
                    } catch (final Exception error) {
                        json.add("error", error.getMessage());
                    } finally {
                        json.close();
                    }
                }
            } catch (final FileUploadException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }
}
