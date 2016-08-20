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

package org.fluidity.wages.http.json;

import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JsonObjectTest extends Simulator {

    @Test
    public void testBooleanEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Object.Root json = JsonOutput.object(64, value::append);

        json.add("a", true);
        json.add("b", false);

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":true,\"b\":false}");
    }

    @Test
    public void testIntegerEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Object.Root json = JsonOutput.object(64, value::append);

        json.add("a", 0);
        json.add("b", 1);
        json.add("c", -1);
        json.add("d", Integer.MAX_VALUE);
        json.add("e", Integer.MIN_VALUE);

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":0,\"b\":1,\"c\":-1,\"d\":2147483647,\"e\":-2147483648}");
    }

    @Test
    public void testFloatingPointNumberEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Object.Root json = JsonOutput.object(64, value::append);

        json.add("a", 0.0);
        json.add("b", 1.0);
        json.add("c", -1.0);
        json.add("d", Float.MAX_VALUE);
        json.add("e", Float.MIN_VALUE);

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":0,\"b\":1,\"c\":-1,\"d\":3.4028234663852886E38,\"e\":1.401298464324817E-45}");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInfinityEncoding() throws Exception {
        final JsonOutput.Object.Root json = JsonOutput.object(64, new StringBuilder()::append);

        json.add("a", Double.POSITIVE_INFINITY);

        assert false;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNaNEncoding() throws Exception {
        final JsonOutput.Object.Root json = JsonOutput.object(64, new StringBuilder()::append);

        json.add("a", Double.NaN);

        assert false;
    }

    @Test
    public void testEmptyTextEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Object.Root json = JsonOutput.object(64, value::append);

        json.add("a", "");

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":\"\"}");
    }

    @Test
    public void testNullEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Object.Root json = JsonOutput.object(64, value::append);

        json.missing("a");

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":null}");
    }

    @Test
    public void testEscapedCharacterEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Object.Root json = JsonOutput.object(64, value::append);

        json.add("a", "a\"b");
        json.add("b", "a\nb");
        json.add("c", "a\bb");
        json.add("d", "a\fb");
        json.add("e", "a\rb");
        json.add("f", "a\tb");

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":\"a\\\"b\",\"b\":\"a\\nb\",\"c\":\"a\\bb\",\"d\":\"a\\fb\",\"e\":\"a\\rb\",\"f\":\"a\\tb\"}");
    }

    @Test
    public void testControlCharacterEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Object.Root json = JsonOutput.object(64, value::append);

        json.add("a", "\u0000\u0002\u0080\u0010\u009f");

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":\"\\u0000\\u0002\\u0080\\u0010\\u009f\"}");
    }

    @Test
    public void testClosesAllOpenContainers() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Object.Root json = JsonOutput.object(4, value::append);

        json.array("a").object().array("b").object().array("c");

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":[{\"b\":[{\"c\":[]}]}]}");
    }

    @Test
    public void testInvokesCloseCallback() throws Exception {
        final JsonOutput.Object.Root json = JsonOutput.object(4, ignored -> {});
        final Runnable callback = dependencies().normal(Runnable.class);

        callback.run();
        EasyMock.expectLastCall();

        verify(() -> json.close(callback));
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*closed.*")
    public void testRejectsAdditionAfterClose() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Object.Root json = JsonOutput.object(4, value::append);
        final JsonOutput.Object b;

        try {
            json.add("a", "a");
            b = json.object("b");
            b.add("c", "c");
            json.add("d", "d");
        } catch (final Exception e) {
            throw new AssertionError(e);
        }

        try {
            b.add("e", "e");        // this should not succeed
        } finally {
            json.close();

            // we should still get a valid JSON
            Assert.assertEquals(value.toString(), "{\"a\":\"a\",\"b\":{\"c\":\"c\"},\"d\":\"d\"}");

            try {
                json.array("x");    // this should not succeed
                assert false;
            } catch (final IllegalStateException e) {
                Assert.assertTrue(e.getMessage().contains("closed"));
            }

            try {
                json.close();       // this should not succeed
                assert false;
            } catch (final IllegalStateException e) {
                Assert.assertTrue(e.getMessage().contains("closed"));
            }
        }
    }
}
