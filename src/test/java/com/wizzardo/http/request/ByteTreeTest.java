package com.wizzardo.http.request;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author: wizzardo
 * Date: 8/30/14
 */
public class ByteTreeTest {

    @Test
    public void test() {
        ByteTree tree = new ByteTree();

        tree.append("foo");
        tree.append("bar");
        tree.append("foobar");
        tree.append("barfoo");

        Assert.assertEquals("foo", tree.getRoot().get("foo".getBytes()));
        Assert.assertEquals("bar", tree.getRoot().get("bar".getBytes()));
        Assert.assertEquals("foobar", tree.getRoot().get("foobar".getBytes()));
        Assert.assertEquals("barfoo", tree.getRoot().get("barfoo".getBytes()));

        Assert.assertEquals("foo", get("foo".getBytes(), tree));
        Assert.assertEquals("bar", get("bar".getBytes(), tree));
        Assert.assertEquals("foobar", get("foobar".getBytes(), tree));
        Assert.assertEquals("barfoo", get("barfoo".getBytes(), tree));
    }

    private String get(byte[] bytes, ByteTree tree) {
        ByteTree.Node node = tree.getRoot();
        for (byte b : bytes) {
            assert node != null;
            node = node.next(b);
        }
        return node.value;
    }

}
