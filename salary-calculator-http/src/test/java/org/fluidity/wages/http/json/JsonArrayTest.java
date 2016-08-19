package org.fluidity.wages.http.json;

import org.testng.Assert;
import org.testng.annotations.Test;

public class JsonArrayTest {

    @Test
    public void testBooleanEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Array json = JsonStream.array(64, value::append);

        json.add(true);
        json.add(false);

        json.close();

        Assert.assertEquals(value.toString(), "[true,false]");
    }

    @Test
    public void testIntegerEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Array json = JsonStream.array(64, value::append);

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

        final JsonStream.Array json = JsonStream.array(64, value::append);

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
        final JsonStream.Array json = JsonStream.array(64, new StringBuilder()::append);

        json.add(Double.POSITIVE_INFINITY);

        assert false;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNaNEncoding() throws Exception {
        final JsonStream.Array json = JsonStream.array(64, new StringBuilder()::append);

        json.add(Double.NaN);

        assert false;
    }

    @Test
    public void testEmptyTextEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Array json = JsonStream.array(64, value::append);

        json.add("");

        json.close();

        Assert.assertEquals(value.toString(), "[\"\"]");
    }

    @Test
    public void testNullEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Array json = JsonStream.array(64, value::append);

        json.missing("a");

        json.close();

        Assert.assertEquals(value.toString(), "[null]");
    }

    @Test
    public void testEscapedCharacterEncodig() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Array json = JsonStream.array(64, value::append);

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

        final JsonStream.Array json = JsonStream.array(64, value::append);

        json.add("\u0000\u0002\u0080\u0010\u009f");

        json.close();

        Assert.assertEquals(value.toString(), "[\"\\u0000\\u0002\\u0080\\u0010\\u009f\"]");
    }

    @Test
    public void testClosesAllOpenContainers() throws Exception {
        final StringBuilder value = new StringBuilder();

        final JsonStream.Array json = JsonStream.array(4, value::append);

        json.object().array("a").object().array("b").object();

        json.close();

        Assert.assertEquals(value.toString(), "[{\"a\":[{\"b\":[{}]}]}]");
    }
}
