package com.wizzardo.http.framework;

import com.wizzardo.http.ServerTest;
import com.wizzardo.http.framework.message.MessageBundle;
import com.wizzardo.http.framework.message.MessageSource;
import org.junit.Before;

/**
 * Created by wizzardo on 03.05.15.
 */
public class WebApplicationTest extends ServerTest<WebApplication> {

    @Before
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        System.out.println("setUp " + name.getMethodName());
        server = new WebApplication(null, port, context, workers) {
            @Override
            protected MessageBundle initMessageSource() {
                return WebApplicationTest.this.initMessageSource(super.initMessageSource());
            }
        };
        server.setIoThreadsCount(1);
        server.start();
    }

    protected MessageBundle initMessageSource(MessageBundle bundle) {
        return bundle;
    }
}
