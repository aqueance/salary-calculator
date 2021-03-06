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

public class JsonArrayTest extends Simulator {

    @Test
    public void testBooleanEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Array.Root json = JsonOutput.array(64, value::append);

        json.add(true);
        json.add(false);

        json.close();

        Assert.assertEquals(value.toString(), "[true,false]");
    }

    @Test
    public void testIntegerEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Array.Root json = JsonOutput.array(64, value::append);

        json.add(0);
        json.add(1);
        json.add(-1);
        json.add(Integer.MAX_VALUE);
        json.add(Integer.MIN_VALUE);

        json.close();

        Assert.assertEquals(value.toString(), "[0,1,-1,2147483647,-2147483648]");
    }

    @Test
    public void testFloatingPointNumberEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Array.Root json = JsonOutput.array(64, value::append);

        json.add(0.0);
        json.add(1.0);
        json.add(-1.0);
        json.add(Float.MAX_VALUE);
        json.add(Float.MIN_VALUE);

        json.close();

        Assert.assertEquals(value.toString(), "[0,1,-1,3.4028234663852886E38,1.401298464324817E-45]");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInfinityEncoding() throws Exception {
        final JsonOutput.Array.Root json = JsonOutput.array(64, new StringBuilder()::append);

        json.add(Double.POSITIVE_INFINITY);

        assert false;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNaNEncoding() throws Exception {
        final JsonOutput.Array.Root json = JsonOutput.array(64, new StringBuilder()::append);

        json.add(Double.NaN);

        assert false;
    }

    @Test
    public void testEmptyTextEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Array.Root json = JsonOutput.array(64, value::append);

        json.add("");

        json.close();

        Assert.assertEquals(value.toString(), "[\"\"]");
    }

    @Test
    public void testNullEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Array.Root json = JsonOutput.array(64, value::append);

        json.missing("a");

        json.close();

        Assert.assertEquals(value.toString(), "[null]");
    }

    @Test
    public void testEscapedCharacterEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Array.Root json = JsonOutput.array(64, value::append);

        json.add("a\"b");
        json.add("a\nb");
        json.add("a\bb");
        json.add("a\fb");
        json.add("a\rb");
        json.add("a\tb");

        json.close();

        Assert.assertEquals(value.toString(), "[\"a\\\"b\",\"a\\nb\",\"a\\bb\",\"a\\fb\",\"a\\rb\",\"a\\tb\"]");
    }

    @Test
    public void testControlCharacterEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Array.Root json = JsonOutput.array(64, value::append);

        json.add("\u0000\u0002\u0080\u0010\u009f");

        json.close();

        Assert.assertEquals(value.toString(), "[\"\\u0000\\u0002\\u0080\\u0010\\u009f\"]");
    }

    @Test
    public void testClosesAllOpenContainers() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Array.Root json = JsonOutput.array(4, value::append);

        json.object().array("a").object().array("b").object();

        json.close();

        Assert.assertEquals(value.toString(), "[{\"a\":[{\"b\":[{}]}]}]");
    }

    @Test
    public void testInvokesCloseCallback() throws Exception {
        final JsonOutput.Array.Root json = JsonOutput.array(4, ignored -> {});
        final Runnable callback = dependencies().normal(Runnable.class);

        callback.run();
        EasyMock.expectLastCall();

        verify(() -> json.close(callback));
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*closed.*")
    public void testRejectsAdditionAfterClose() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonOutput.Array.Root json = JsonOutput.array(4, value::append);
        final JsonOutput.Array b;

        try {
            json.add("a");
            b = json.array();
            b.add("c");
            json.add("d");
        } catch (final Exception e) {
            throw new AssertionError(e);
        }

        try {
            b.add("e");             // this should not succeed
        } finally {
            json.close();

            // we should still get a valid JSON
            Assert.assertEquals(value.toString(), "[\"a\",[\"c\"],\"d\"]");

            try {
                json.object();      // this should not succeed
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
