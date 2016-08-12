package org.fluidity.wages.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Function;

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
@Component(api = ResourceHandlerFactory.class)
class ResourceHandlerFactory implements Function<Vertx, Handler<RoutingContext>> {

    private static final int CHUNK_SIZE = 16384;

    @Override
    public Handler<RoutingContext> apply(final Vertx vertx) {
        return context -> {
            final HttpServerRequest request = context.request();
            final HttpServerResponse response = context.response();

            final String uri = request.uri();
            final String path = "/webroot" + (uri.equals("/") ? "/index.html" : uri);

            final URL resource = ClassLoaders.findResource(getClass(), path);

            if (resource == null) {
                response.setStatusCode(404);
                response.end();
            } else {
                vertx.executeBlocking(future -> {
                    try {
                        final URLConnection connection = resource.openConnection();
                        final InputStream stream = connection.getInputStream();

                        final String contentType = connection.getContentType();
                        final int contentLength = connection.getContentLength();

                        response.setStatusCode(200);
                        response.putHeader("Content-Type", contentType.startsWith("text/") ? contentType + "; charset=utf-8" : contentType);
                        response.putHeader("Content-Length", String.valueOf(contentLength));

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
        };
    }
}
