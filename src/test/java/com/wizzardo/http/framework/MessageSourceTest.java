package com.wizzardo.http.framework;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by wizzardo on 21.05.15.
 */
public class MessageSourceTest {

    @Test
    public void test_template() {
        Assert.assertEquals("foo bar", new MessageSource.Template("foo bar").get());
        Assert.assertEquals("foo bar", new MessageSource.Template("foo {0}").get("bar"));
        Assert.assertEquals("foo bar", new MessageSource.Template("{0} bar").get("foo"));
        Assert.assertEquals("foo bar", new MessageSource.Template("{0} {1}").get("foo", "bar"));
        Assert.assertEquals("foo bar", new MessageSource.Template("{1} {0}").get("bar", "foo"));
        Assert.assertEquals("bar bar", new MessageSource.Template("{0} {0}").get("bar"));
        Assert.assertEquals("foo null", new MessageSource.Template("foo {0}").get());
    }
}
