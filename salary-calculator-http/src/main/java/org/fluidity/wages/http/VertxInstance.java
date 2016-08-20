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

import java.util.function.Supplier;

import org.fluidity.composition.Component;

import io.vertx.core.Vertx;

/**
 * Encapsulates the Vert.x instance used by this server.
 */
@Component(api = VertxInstance.class)
final class VertxInstance implements Supplier<Vertx> {

    private final Vertx vertx = Vertx.vertx();

    @Override
    public Vertx get() {
        return vertx;
    }
}
