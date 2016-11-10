package com.wizzardo.http.framework;

import com.wizzardo.http.framework.di.DependencyFactory;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by wizzardo on 03.05.15.
 */
public class WebAppTest {

    @Test
    public void test_args() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        DependencyFactory.get().clear();

        WebApplication app;
        app = new WebApplication(new String[]{"profiles.active=test"});
        Assert.assertEquals("[test]", app.getProfiles().toString());
        Assert.assertEquals(Environment.DEVELOPMENT, app.getEnvironment());

        app = new WebApplication(new String[]{"env=test"});
        Assert.assertEquals("[]", app.getProfiles().toString());
        Assert.assertEquals(Environment.TEST, app.getEnvironment());

        app = new WebApplication(new String[]{"environment=prod"});
        Assert.assertEquals("[]", app.getProfiles().toString());
        Assert.assertEquals(Environment.PRODUCTION, app.getEnvironment());

        app = new WebApplication(new String[]{"foo=bar", "foobar"});
        Assert.assertEquals("[]", app.getProfiles().toString());
        Assert.assertEquals(Environment.DEVELOPMENT, app.getEnvironment());
        Assert.assertEquals(2, app.cliArgs.size());
        Assert.assertEquals("bar", app.cliArgs.get("foo"));
        Assert.assertEquals(null, app.cliArgs.get("foobar"));
    }

}
