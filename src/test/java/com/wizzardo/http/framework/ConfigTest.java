package com.wizzardo.http.framework;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class ConfigTest extends WebApplicationTest {

    @Before
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        System.out.println("setUp " + this.getClass().getSimpleName() + "." + name.getMethodName());
        server = new WebApplication(null, port, context, workers);
        server.setIoThreadsCount(1);
    }

    @Test
    public void test() throws IOException {
        server.setEnvironment(Environment.TEST);
        server.start();

        Assert.assertEquals("value", server.getConfig().get("key"));
        Assert.assertEquals("c", ((Map) server.getConfig().get("a")).get("b"));
        Assert.assertEquals(1, ((Map) server.getConfig().get("a")).get("c"));

        Assert.assertTrue(((Map) server.getConfig().get("environments")).isEmpty());
    }

    @Test
    public void test_environments_test() throws IOException {
        server.setEnvironment(Environment.TEST);
        server.start();

        Assert.assertEquals("test", server.getConfig().get("env"));
        Assert.assertEquals("test", server.getConfig().get("environment"));
    }

    @Test
    public void test_environments_dev() throws IOException {
        server.setEnvironment(Environment.DEVELOPMENT);
        server.start();

        Assert.assertEquals("dev", server.getConfig().get("env"));
        Assert.assertEquals("development", server.getConfig().get("environment"));
    }

    @Test
    public void test_environments_prod() throws IOException {
        server.setEnvironment(Environment.PRODUCTION);
        server.start();

        Assert.assertEquals("prod", server.getConfig().get("env"));
        Assert.assertEquals("production", server.getConfig().get("environment"));
    }
}
