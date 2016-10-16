package com.wizzardo.http.framework;

import com.wizzardo.http.framework.template.Renderer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by wizzardo on 02.05.15.
 */
public class ControllerHandlerTest extends WebApplicationTest {

    public static class HelloController extends Controller {
        public Renderer hello() {
            return renderString("hello!");
        }
    }

    @Test
    public void test() throws IOException {
        server.getUrlMapping()
                .append("/hello", HelloController.class, "hello");

        Assert.assertEquals("hello!", makeRequest("/hello").get().asString());
    }


    public static class TestParametersController extends Controller {
        public Renderer test_1(@Parameter(name = "i") int i) {
            return renderString(String.valueOf(i));
        }

        public Renderer test_2(@Parameter(name = "i") Optional<Integer> i) {
            return renderString(String.valueOf(i.orElse(-1)));
        }
    }

    @Test
    public void test_paramteres_1() throws IOException {
        server.getUrlMapping()
                .append("/test_1", TestParametersController.class, "test_1");

        Assert.assertEquals("1", makeRequest("/test_1").param("i", 1).get().asString());
        Assert.assertEquals("2", makeRequest("/test_1").param("i", 2).get().asString());


        server.getUrlMapping()
                .append("/test_2", TestParametersController.class, "test_2");

        Assert.assertEquals("1", makeRequest("/test_2").param("i", 1).get().asString());
        Assert.assertEquals("2", makeRequest("/test_2").param("i", 2).get().asString());
        Assert.assertEquals("-1", makeRequest("/test_2").get().asString());
    }
}
