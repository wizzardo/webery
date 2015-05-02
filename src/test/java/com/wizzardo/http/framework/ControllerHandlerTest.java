package com.wizzardo.http.framework;

import com.wizzardo.http.ServerTest;
import com.wizzardo.http.UrlHandler;
import com.wizzardo.http.framework.template.Renderer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by wizzardo on 02.05.15.
 */
public class ControllerHandlerTest extends ServerTest {

    public static class HelloController extends Controller {

        public Renderer hello() {
            return renderString("hello!");
        }
    }

    @Test
    public void test() throws IOException {
        handler = new UrlHandler()
                .append("/hello", new ControllerHandler(HelloController.class, "hello"));

        Assert.assertEquals("hello!", makeRequest("/hello").get().asString());
    }
}
