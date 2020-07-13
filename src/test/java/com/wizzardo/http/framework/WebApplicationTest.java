package com.wizzardo.http.framework;

import com.wizzardo.http.ServerTest;
import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.tools.evaluation.Config;
import org.junit.Before;

/**
 * Created by wizzardo on 03.05.15.
 */
public class WebApplicationTest extends ServerTest<WebApplication> {

    @Before
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        DependencyFactory.get().clear();

        System.out.println("setUp " + this.getClass().getSimpleName() + "." + name.getMethodName());
        server = new WebApplication() {
            @Override
            protected MessageBundle initMessageSource() {
                return WebApplicationTest.this.initMessageSource(super.initMessageSource());
            }

            @Override
            protected void loadEnvironmentVariables(Config config) {
                //ignore to speed up tests
            }

            @Override
            protected void loadSystemProperties(Config config) {
                //ignore to speed up tests
            }
        };

        server.setHostname(null);
        server.setPort(port);
        server.setContext(context);
        server.setWorkersCount(workers);

        server.onLoadConfiguration(app -> configure(app.config));

        server.setIoThreadsCount(1);
        server.setEnvironment(Environment.TEST);
        server.start();
    }

    protected void configure(Config config) {
        config.config("server").config("renderer").put("offset", "    ");
        config.config("server").config("renderer").put("withNewline", true);
    }

    protected MessageBundle initMessageSource(MessageBundle bundle) {
        return bundle;
    }
}
