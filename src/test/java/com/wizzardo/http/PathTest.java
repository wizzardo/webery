package com.wizzardo.http;

import com.wizzardo.http.mapping.Path;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by wizzardo on 16.01.15.
 */
public class PathTest {

    private Path parse(String s) {
        return Path.parse(s.getBytes());
    }

    @Test
    public void parsing() {
        Path path;

        path = parse("/");
        Assert.assertEquals("/", path.toString());
        Assert.assertEquals(0, path.length());
        Assert.assertEquals(true, path.isEndsWithSlash());

        path = parse("/foo");
        Assert.assertEquals("/foo", path.toString());
        Assert.assertEquals(1, path.length());
        Assert.assertEquals("foo", path.getPart(0));
        Assert.assertEquals(false, path.isEndsWithSlash());

        path = parse("/foo/");
        Assert.assertEquals("/foo/", path.toString());
        Assert.assertEquals(1, path.length());
        Assert.assertEquals("foo", path.getPart(0));
        Assert.assertEquals(true, path.isEndsWithSlash());

        path = parse("/foo/bar");
        Assert.assertEquals("/foo/bar", path.toString());
        Assert.assertEquals(2, path.length());
        Assert.assertEquals("foo", path.getPart(0));
        Assert.assertEquals("bar", path.getPart(1));
        Assert.assertEquals(false, path.isEndsWithSlash());
    }

    @Test
    public void subPath() {
        Path path;
        Path sub;

        path = parse("/foo/bar");
        sub = path.subPath(1);
        Assert.assertEquals("/bar", sub.toString());
        Assert.assertEquals(1, sub.length());
        Assert.assertEquals("bar", sub.getPart(0));

        path = parse("/foo/bar/key/value");
        sub = path.subPath(1, 3);
        Assert.assertEquals("/bar/key/", sub.toString());
        Assert.assertEquals(2, sub.length());
        Assert.assertEquals("bar", sub.getPart(0));
        Assert.assertEquals("key", sub.getPart(1));

        path = parse("/foo/bar/key/");
        sub = path.subPath(1, 3);
        Assert.assertEquals("/bar/key/", sub.toString());
        Assert.assertEquals(2, sub.length());
        Assert.assertEquals("bar", sub.getPart(0));
        Assert.assertEquals("key", sub.getPart(1));

        path = parse("/foo/bar/key/");
        sub = path.subPath(1, 2);
        Assert.assertEquals("/bar/", sub.toString());
        Assert.assertEquals(1, sub.length());
        Assert.assertEquals("bar", sub.getPart(0));
    }

    @Test
    public void dots() {
        Path path;

        path = parse("/foo/../bar");
        Assert.assertEquals("/foo/../bar", path.toString());
        Assert.assertEquals(1, path.length());
        Assert.assertEquals("bar", path.getPart(0));
        Assert.assertEquals(false, path.isEndsWithSlash());

        path = parse("/foo/foo/../../bar");
        Assert.assertEquals("/foo/foo/../../bar", path.toString());
        Assert.assertEquals(1, path.length());
        Assert.assertEquals("bar", path.getPart(0));
        Assert.assertEquals(false, path.isEndsWithSlash());
    }

    @Test
    public void exceptions() {
        ServerTest.checkException(() -> parse("/foo/../../"), IllegalStateException.class, "can't parse: /foo/../../");
        ServerTest.checkException(() -> parse("/.."), IllegalStateException.class, "can't parse: /..");
        ServerTest.checkException(() -> parse("foo"), IllegalStateException.class, "path must starts with '/'");
    }
}
