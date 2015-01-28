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

        Assert.assertSame("foo", tree.getRoot().get("foo".getBytes()));
        Assert.assertSame("bar", tree.getRoot().get("bar".getBytes()));
        Assert.assertSame("foobar", tree.getRoot().get("foobar".getBytes()));
        Assert.assertSame("barfoo", tree.getRoot().get("barfoo".getBytes()));

        Assert.assertSame("foo", get("foo".getBytes(), tree));
        Assert.assertSame("bar", get("bar".getBytes(), tree));
        Assert.assertSame("foobar", get("foobar".getBytes(), tree));
        Assert.assertSame("barfoo", get("barfoo".getBytes(), tree));
    }

    @Test
    public void testIgnoreCase(){
        ByteTree tree = new ByteTree();

        String foo = "foo";
        tree.appendIgnoreCase(foo);

        Assert.assertSame(foo, tree.getRoot().get("foo".getBytes()));
        Assert.assertSame(foo, tree.getRoot().get("foO".getBytes()));
        Assert.assertSame(foo, tree.getRoot().get("fOo".getBytes()));
        Assert.assertSame(foo, tree.getRoot().get("fOO".getBytes()));
        Assert.assertSame(foo, tree.getRoot().get("Foo".getBytes()));
        Assert.assertSame(foo, tree.getRoot().get("FOo".getBytes()));
        Assert.assertSame(foo, tree.getRoot().get("FOO".getBytes()));
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
