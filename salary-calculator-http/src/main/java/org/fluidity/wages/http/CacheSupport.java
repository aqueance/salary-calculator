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
import java.net.URLConnection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fluidity.composition.Component;

/**
 * Handles HTTP 1.1 cache headers.
 */
@Component
@SuppressWarnings("WeakerAccess")
final class CacheSupport {

    /**
     * Checks the <code>If-Modified-Since</code> header against the last modified time of the resource, and if they match, sends a HTTP status 304 and ends the
     * response, else sends an HTTP status 200 and sets the appropriate caching headers for the next time the resource is requested.
     *
     * @param resource the resource to check the timestamp of.
     * @param request  the HTTP request.
     * @param response the HTTP response.
     *
     * @return <code>true</code> if the resource is up to date at the client, <code>false</code> otherwise.
     */
    public boolean handled(final URLConnection resource, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final long lastModified = resource.getLastModified();

        final boolean cached = request.getDateHeader("If-Modified-Since") == lastModified;

        if (cached) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        } else {
            response.setStatus(HttpServletResponse.SC_OK);

            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "private");

            if (lastModified != -1) {
                response.addDateHeader("Last-Modified", lastModified);
            }
        }

        return cached;
    }
}
