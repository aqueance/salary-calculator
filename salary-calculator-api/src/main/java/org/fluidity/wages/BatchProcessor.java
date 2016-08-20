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

package org.fluidity.wages;

import java.util.function.Consumer;

/**
 * Consumes and processes a stream of objects. The two extended interfaces together allow using the processor in a try-with-resource construct, like so:
 * <pre>
 *     try (final BatchProcessor&lt;&hellip;&gt; processor = &hellip;) {
 *         processor.accept(&hellip;)
 *     }
 * </pre>
 *
 * @param <T> the type of the input to the processor.
 */
public interface BatchProcessor<T> extends Consumer<T>, AutoCloseable {

    /**
     * Signals the receiver that the caller has completed one batch. The receiver must send its accumulated state to downstream consumers.
     */
    void flush();

    /**
     * Flushes any pending state computed so far and closes this processor. The instance will not be usable afterwards.
     */
    @Override
    void close();
}
