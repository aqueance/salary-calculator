package org.fluidity.wages.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.foundation.ClassLoaders;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * TODO: javadoc...
 */
@Component(api = ResourceHandler.class)
class ResourceHandler implements Handler<RoutingContext> {

    private static final int CHUNK_SIZE = 16384;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

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

    private final Vertx vertx;

    public ResourceHandler(final VertxInstance vertx) {
        this.vertx = vertx.get();

        URLConnection.setFileNameMap(fileName -> MIME_TYPES.get(fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()));
    }

    @Override
    public void handle(final RoutingContext context) {
        final HttpServerRequest request = context.request();
        final HttpServerResponse response = context.response();

        final String uri = request.uri();
        final String query = "/webroot" + (uri.equals("/") ? "/index.html" : uri);

        final int qm = query.indexOf('?');
        final String path = qm < 0 ? query : query.substring(1, qm);

        final URL resource = ClassLoaders.findResource(getClass(), path);

        if (resource == null) {
            context.next();
        } else {

            // TODO: handle caching headers

            vertx.executeBlocking(future -> {
                try {
                    final URLConnection connection = resource.openConnection();
                    final InputStream stream = connection.getInputStream();

                    final String contentType = connection.getContentType();
                    final int contentLength = connection.getContentLength();

                    response.setStatusCode(200);
                    response.putHeader("Content-Type", contentType.startsWith("text/") ? contentType + "; charset=utf-8" : contentType);
                    response.putHeader("Content-Length", String.valueOf(contentLength));

                    // TODO: allow caching

                    final byte[] bytes = new byte[CHUNK_SIZE];
                    for (int len; (len = stream.read(bytes)) != -1; ) {
                        response.write(Buffer.buffer(len).appendBytes(bytes, 0, len));
                    }
                } catch (final IOException error) {
                    assert !response.headWritten() : error;
                    response.setStatusCode(500);
                    response.end(error.getMessage());
                } finally {
                    response.end();

                    future.complete();
                }
            }, null);
        }
    }
}
