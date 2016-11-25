package com.wizzardo.http.framework.message;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class ArgsTest {

    @Test
    public void test_null() {
        Args args = Args.create((List) null);
        Assert.assertEquals(null, args.get(0));
    }

    @Test
    public void test_null2() {
        Args args = Args.create((Object[]) null);
        Assert.assertEquals(null, args.get(0));
    }

    @Test
    public void test_index_out_of_bounds() {
        Args args = Args.create(Collections.emptyList());
        Assert.assertEquals(null, args.get(0));
    }

    @Test
    public void test_index_out_of_bounds2() {
        Args args = Args.create();
        Assert.assertEquals(null, args.get(0));
    }

    @Test
    public void test_array() {
        Args args = Args.create("foo");
        Assert.assertEquals("foo", args.get(0));
    }

    @Test
    public void test_collection() {
        Args args = Args.create(Collections.singletonList("foo"));
        Assert.assertEquals("foo", args.get(0));
    }
}