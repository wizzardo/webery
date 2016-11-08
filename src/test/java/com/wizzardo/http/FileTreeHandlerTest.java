package com.wizzardo.http;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by wizzardo on 09/11/16.
 */
public class FileTreeHandlerTest extends ServerTest {

    @Test
    public void test_prefix(){
        FileTreeHandler handler;

        handler = new FileTreeHandler("/", "");
        Assert.assertEquals("", handler.prefix);

        handler = new FileTreeHandler("/", "/");
        Assert.assertEquals("", handler.prefix);

        handler = new FileTreeHandler("/", "prefix");
        Assert.assertEquals("/prefix", handler.prefix);

        handler = new FileTreeHandler("/", "prefix/");
        Assert.assertEquals("/prefix", handler.prefix);

        handler = new FileTreeHandler("/", "/prefix/");
        Assert.assertEquals("/prefix", handler.prefix);

        handler = new FileTreeHandler("/", "/prefix");
        Assert.assertEquals("/prefix", handler.prefix);
    }
}
