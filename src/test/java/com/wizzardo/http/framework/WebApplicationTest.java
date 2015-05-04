package com.wizzardo.http.framework;

import com.wizzardo.http.ServerTest;
import org.junit.Before;

/**
 * Created by wizzardo on 03.05.15.
 */
public class WebApplicationTest extends ServerTest {

    @Before
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        System.out.println("setUp " + name.getMethodName());
        server = new WebApplication<>(null, port, context, workers);
        server.setIoThreadsCount(1);
        server.start();
    }
}
