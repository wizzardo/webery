package com.wizzardo.http.framework.template;

import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.io.IOTools;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Test
    public void test_getResourceFile() throws IOException {
        LocalResourcesTools resources = new LocalResourcesTools();

        FileTools.text("/tmp/test_resource", "foo");
        AtomicInteger counter = new AtomicInteger();
        resources.getResourceFile("/tmp/test_resource", file -> {
            Assert.assertEquals("foo", FileTools.text(file));
            counter.incrementAndGet();
        });
        Assert.assertEquals(1, counter.get());


        resources.getResourceFile("/tmp/test_resource_not_exists", file -> {
            Assert.assertEquals("foo", FileTools.text(file));
            counter.incrementAndGet();
        });
        Assert.assertEquals(1, counter.get());
    }

    @Test
    public void test_getResourceFile_2() throws IOException {
        LocalResourcesTools resources = new LocalResourcesTools();

        Assert.assertEquals("foo", FileTools.text(resources.getResourceFile("test_resource")));
    }

    @Test
    public void test_getResourceFile_3() throws IOException {
        LocalResourcesTools resources = new LocalResourcesTools();
        resources.addResourcesDir(new File("/tmp"));

        FileTools.text("/tmp/test_resource_foo", "foo");
        Assert.assertEquals("foo", FileTools.text(resources.getResourceFile("test_resource_foo")));
    }
}