package com.wizzardo.http.framework;

import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.ResourceTools;
import com.wizzardo.http.framework.template.TestResourcesTools;
import com.wizzardo.tools.evaluation.Config;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class ConfigTest extends WebApplicationTest {

    @Before
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        System.out.println("setUp " + this.getClass().getSimpleName() + "." + name.getMethodName());
        server = new WebApplication() {
            @Override
            protected ResourceTools createResourceTools() {
                return new TestResourcesTools();
            }
        };
        server.setHostname(null);
        server.setPort(port);
        server.setContext(context);
        server.setWorkersCount(workers);
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

    public static class ConfigItem implements Configuration {

        String key;

        public String prefix() {
            return "item";
        }
    }

    @Test
    public void test_binding() {
        server.start();

        ConfigItem configItem = DependencyFactory.get(ConfigItem.class);
        Assert.assertEquals("value", configItem.key);
    }


    @Test
    public void test_profiles_1() throws IOException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException, InterruptedException {
        server.config.config("profiles").config("profile_A").put("key", "A");
        server.config.config("profiles").config("profile_B").put("key", "B");
        server.start();

        Assert.assertEquals("value", server.getConfig().get("key"));
        tearDown();

        setUp();
        server.config.config("profiles").config("profile_A").put("key", "A");
        server.config.config("profiles").config("profile_B").put("key", "B");
        server.addProfile("profile_A");
        server.start();
        Assert.assertEquals("A", server.getConfig().get("key"));
        tearDown();

        setUp();
        server.config.config("profiles").config("profile_A").put("key", "A");
        server.config.config("profiles").config("profile_B").put("key", "B");
        server.addProfile("profile_A");
        server.addProfile("profile_B");
        server.start();
        Assert.assertEquals("B", server.getConfig().get("key"));
    }

    @Test
    public void test_profiles_2() throws IOException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException, InterruptedException {
        server.config.config("profiles").config("profile_A").put("key", "A");
        server.config.config("profiles").config("profile_B").put("key", "B");
        server.config.config("profiles").config("profile_C").config("profiles").config("profile_A").put("key", "C");
        server.addProfile("profile_A");
        server.addProfile("profile_C");
        server.addProfile("profile_B");
        server.start();
        Assert.assertEquals("B", server.getConfig().get("key"));
        tearDown();

        setUp();
        server.config.config("profiles").config("profile_A").put("key", "A");
        server.config.config("profiles").config("profile_C").config("profiles").config("profile_A").put("key", "C");
        server.addProfile("profile_A");
        server.addProfile("profile_C");
        server.start();
        Assert.assertEquals("C", server.getConfig().get("key"));
        tearDown();


        setUp();
        server.config.config("profiles").config("profile_A").put("key", "A");
        server.config.config("profiles").config("profile_C").config("profiles").config("profile_A").put("key", "C");
        server.addProfile("profile_C");
        server.addProfile("profile_A");
        server.start();
        Assert.assertEquals("A", server.getConfig().get("key"));
    }

    @Test
    public void test_profiles_3() throws IOException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException, InterruptedException {
        server.config.config("environments").config("dev").put("key", "dev");
        server.config.config("environments").config("prod").put("key", "prod");
        server.config.config("profiles").config("profile_A").config("environments").config("dev").put("key", "dev_A");
        server.config.config("profiles").config("profile_A").config("environments").config("prod").put("key", "prod_A");
        server.config.config("profiles").config("profile_B").config("environments").config("dev").put("key", "dev_B");
        server.config.config("profiles").config("profile_B").config("environments").config("prod").put("key", "prod_B");
        server.setEnvironment(Environment.DEVELOPMENT);
        server.start();
        Assert.assertEquals("dev", server.getConfig().get("key"));
        tearDown();

        setUp();
        server.config.config("environments").config("dev").put("key", "dev");
        server.config.config("environments").config("prod").put("key", "prod");
        server.config.config("profiles").config("profile_A").config("environments").config("dev").put("key", "dev_A");
        server.config.config("profiles").config("profile_A").config("environments").config("prod").put("key", "prod_A");
        server.config.config("profiles").config("profile_B").config("environments").config("dev").put("key", "dev_B");
        server.config.config("profiles").config("profile_B").config("environments").config("prod").put("key", "prod_B");
        server.setEnvironment(Environment.PRODUCTION);
        server.addProfile("profile_A");
        server.start();
        Assert.assertEquals("prod_A", server.getConfig().get("key"));
        tearDown();

        setUp();
        server.config.config("environments").config("dev").put("key", "dev");
        server.config.config("environments").config("prod").put("key", "prod");
        server.config.config("profiles").config("profile_A").config("environments").config("dev").put("key", "dev_A");
        server.config.config("profiles").config("profile_A").config("environments").config("prod").put("key", "prod_A");
        server.config.config("profiles").config("profile_B").config("environments").config("dev").put("key", "dev_B");
        server.config.config("profiles").config("profile_B").config("environments").config("prod").put("key", "prod_B");
        server.setEnvironment(Environment.DEVELOPMENT);
        server.addProfile("profile_B");
        server.start();
        Assert.assertEquals("dev_B", server.getConfig().get("key"));
    }

    @Test
    public void test_profiles_4() throws IOException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException, InterruptedException {
        server.config.config("environments").config("dev").config("profiles").config("profile_A").put("key", "dev_A");
        server.config.config("environments").config("dev").config("profiles").config("profile_B").put("key", "dev_B");
        server.config.config("environments").config("prod").config("profiles").config("profile_A").put("key", "prod_A");
        server.config.config("environments").config("prod").config("profiles").config("profile_B").put("key", "prod_B");
        server.setEnvironment(Environment.DEVELOPMENT);
        server.addProfile("profile_B");
        server.start();
        Assert.assertEquals("dev_B", server.getConfig().get("key"));
    }

    @Test
    public void test_profiles_5() throws IOException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException, InterruptedException {
        server.setEnvironment(Environment.TEST);
        server.start();
        Assert.assertEquals("bar", server.getConfig().config("foo").get("v"));
        Assert.assertTrue(server.getConfig().get("bar") instanceof Config);
        Assert.assertTrue(((Config) server.getConfig().get("bar")).isEmpty());
    }

    @Test
    public void test_profiles_6() throws IOException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException, InterruptedException {
        server.setEnvironment(Environment.DEVELOPMENT);
        server.start();
        Assert.assertEquals("foobar", server.getConfig().config("foo").get("v"));
        Assert.assertEquals("foobar", server.getConfig().config("bar").get("v").toString());
    }

    @Test
    public void test_profiles_7() throws IOException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException, InterruptedException {
        server.setEnvironment(Environment.DEVELOPMENT);
        server.addProfile("a");
        server.addProfile("b");
        server.start();
        Assert.assertEquals("b", server.getConfig().config("sub").get("key"));
        tearDown();

        setUp();
        server.addProfile("b");
        server.addProfile("a");
        server.start();
        Assert.assertEquals("b", server.getConfig().config("sub").get("key"));
    }
}
