package org.fluidity.wages.http;

import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.fluidity.composition.Component;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Handles HTTP 1.1 cache headers.
 */
@Component
@SuppressWarnings("WeakerAccess")
final class CacheSupport {

    private static final DateTimeFormatter HTTP_DATES = DateTimeFormatter.RFC_1123_DATE_TIME;

    /**
     * Checks the <code>If-Modified-Since</code> header against the last modified time of the resource, and if they match, sends a HTTP status 304 and ends the
     * response, else sends an HTTP status 200 and sets the appropriate caching headers for the next time the resource is requested.
     *
     * @param resource the resource to check the timestamp of.
     * @param context  the Vert.x request context.
     *
     * @return <code>true</code> if the resource is up to date at the client, <code>false</code> otherwise.
     */
    public boolean handle(final URLConnection resource, final RoutingContext context) {
        final HttpServerRequest request = context.request();
        final HttpServerResponse response = context.response();

        final long lastModified = resource.getLastModified();

        final boolean cached = parseDateHeader(request.getHeader("If-Modified-Since")) == lastModified;

        if (cached) {
            response.setStatusCode(304);
            response.end();
        } else {
            response.setStatusCode(200);

            response.putHeader("Pragma", "no-cache");
            response.putHeader("Cache-Control", "private");

            if (lastModified != -1) {
                response.putHeader("Last-Modified", formatDateHeader(lastModified));
            }
        }

        return cached;
    }

    private String formatDateHeader(final long timestamp) {
        return HTTP_DATES.format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC));
    }

    private long parseDateHeader(final String header) {
        return header == null ? 0 : Instant.from(HTTP_DATES.parse(header)).toEpochMilli();
    }
}
