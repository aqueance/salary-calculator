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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fluidity.composition.Component;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.IOStreams;
import org.fluidity.foundation.Strings;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

/**
 * Serves static resources from <code>/webroot</code>.
 */
@Component(api = ResourceHandler.class)
class ResourceHandler extends AbstractHandler {

    private static final int CHUNK_SIZE = 16384;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    private static final String WEBROOT = "/webroot";
    private static final String INDEX_HTML = "/index.html";

    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("css", "text/css");
        MIME_TYPES.put("js", "application/javascript");
        MIME_TYPES.put("woff", "application/font-woff");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("otf", "font/opentype");
        MIME_TYPES.put("eot", "application/vnd.ms-fontobject");
        MIME_TYPES.put("ttf", "application/font-sfnt");
        MIME_TYPES.put("svg", "image/svg+xml");
    }

    private final CacheSupport caching;

    public ResourceHandler(final CacheSupport caching) {
        this.caching = caching;

        URLConnection.setFileNameMap(fileName -> MIME_TYPES.get(fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()));
    }

    @Override
    public void handle(final String uri, final Request control, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        final String query = WEBROOT + (uri.equals("/") ? INDEX_HTML : uri);

        final int qm = query.indexOf('?');
        final String path = qm < 0 ? query : query.substring(1, qm);

        final URL url = ClassLoaders.findResource(getClass(), path);
        final HttpMethod method = HttpMethod.fromString(request.getMethod());

        if (url == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else if (method != HttpMethod.GET && method != HttpMethod.HEAD) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.addHeader("Allow", "GET, HEAD");
        } else {
            final ServletOutputStream output = response.getOutputStream();

            try {
                final URLConnection resource = url.openConnection();

                if (!caching.handled(resource, request, response)) {
                    final String contentType = resource.getContentType();
                    assert contentType != null : path;

                    final int contentLength = resource.getContentLength();

                    if (contentType.startsWith("text/")) {
                        response.setCharacterEncoding(Strings.UTF_8.name());
                    }

                    response.setContentType(contentType);
                    response.setContentLength(contentLength);

                    if (HttpMethod.GET == method) {
                        try (final InputStream stream = resource.getInputStream()) {
                            IOStreams.send(stream, new byte[CHUNK_SIZE], output::write);
                        } catch (final IOException error) {
                            // ignored: client disconnected or not reachable for some other reason
                        }
                    }
                }
            } catch (final IOException error) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain");
                output.write(error.getMessage().getBytes(Strings.UTF_8));
            }
        }
    }
}
