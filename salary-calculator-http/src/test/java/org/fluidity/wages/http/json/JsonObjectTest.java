package org.fluidity.wages.http.json;

import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JsonObjectTest extends Simulator {

    @Test
    public void testBooleanEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Object json = JsonStream.object(64, value::append);

        json.add("a", true);
        json.add("b", false);

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":true,\"b\":false}");
    }

    @Test
    public void testIntegerEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Object json = JsonStream.object(64, value::append);

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

        final JsonStream.Object json = JsonStream.object(64, value::append);

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
        final JsonStream.Object json = JsonStream.object(64, new StringBuilder()::append);

        json.add("a", Double.POSITIVE_INFINITY);

        assert false;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNaNEncoding() throws Exception {
        final JsonStream.Object json = JsonStream.object(64, new StringBuilder()::append);

        json.add("a", Double.NaN);

        assert false;
    }

    @Test
    public void testEmptyTextEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Object json = JsonStream.object(64, value::append);

        json.add("a", "");

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":\"\"}");
    }

    @Test
    public void testNullEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Object json = JsonStream.object(64, value::append);

        json.missing("a");

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":null}");
    }

    @Test
    public void testEscapedCharacterEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Object json = JsonStream.object(64, value::append);

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

        final JsonStream.Object json = JsonStream.object(64, value::append);

        json.add("a", "\u0000\u0002\u0080\u0010\u009f");

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":\"\\u0000\\u0002\\u0080\\u0010\\u009f\"}");
    }

    @Test
    public void testClosesAllOpenContainers() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Object json = JsonStream.object(4, value::append);

        json.array("a").object().array("b").object().array("c");

        json.close();

        Assert.assertEquals(value.toString(), "{\"a\":[{\"b\":[{\"c\":[]}]}]}");
    }

    @Test
    public void testInvokesCloseCallback() throws Exception {
        final JsonStream.Object json = JsonStream.object(4, ignored -> {});
        final Runnable callback = dependencies().normal(Runnable.class);

        callback.run();
        EasyMock.expectLastCall();

        verify(() -> json.close(callback));
    }
}
