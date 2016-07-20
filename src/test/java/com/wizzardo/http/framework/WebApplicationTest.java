package com.wizzardo.http.framework;

import com.wizzardo.http.ServerTest;
import com.wizzardo.http.framework.message.MessageBundle;
import org.junit.Before;

/**
 * Created by wizzardo on 03.05.15.
 */
public class WebApplicationTest extends ServerTest<WebApplication> {

    @Before
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        System.out.println("setUp " + this.getClass().getSimpleName() + "." + name.getMethodName());
        server = new WebApplication() {
            @Override
            protected MessageBundle initMessageSource() {
                return WebApplicationTest.this.initMessageSource(super.initMessageSource());
            }
        };

        server.setHostname(null);
        server.setPort(port);
        server.setContext(context);
        server.setWorkersCount(workers);

        server.setIoThreadsCount(1);
        server.setEnvironment(Environment.TEST);
        server.start();
    }

    protected MessageBundle initMessageSource(MessageBundle bundle) {
        return bundle;
    }
}
