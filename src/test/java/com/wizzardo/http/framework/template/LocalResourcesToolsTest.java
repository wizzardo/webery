package com.wizzardo.http.framework.template;

import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.io.IOTools;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by wizzardo on 14/11/16.
 */
public class LocalResourcesToolsTest {

    @Test
    public void test_getResource() throws IOException {
        LocalResourcesTools resources = new LocalResourcesTools();

        Assert.assertEquals("foo", resources.getResourceAsString("test_resource"));

        Assert.assertEquals("foo", resources.getResourceAsString("/test_resource"));
    }

    @Test
    public void test_getResource_2() throws IOException {
        LocalResourcesTools resources = new LocalResourcesTools();

        FileTools.text("/tmp/test_resource", "foo");

        Assert.assertEquals("foo", resources.getResourceAsString("/tmp/test_resource"));

        try {
            resources.getResource("/tmp/test_resource_2");
            Assert.assertTrue(false);
        } catch (FileNotFoundException e) {
            Assert.assertEquals("file /tmp/test_resource_2 not found", e.getMessage());
        }
    }
}