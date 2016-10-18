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


    public static class TestParametersPrimitivesController extends Controller {
        public Renderer test_int(@Parameter(name = "v") int v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_long(@Parameter(name = "v") long v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_short(@Parameter(name = "v") short v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_byte(@Parameter(name = "v") byte v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_boolean(@Parameter(name = "v") boolean v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_float(@Parameter(name = "v") float v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_double(@Parameter(name = "v") double v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_char(@Parameter(name = "v") char v) {
            return renderString(String.valueOf(v));
        }
    }

    @Test
    public void test_paramteres_primitives() throws IOException {
        server.getUrlMapping()
                .append("/int", TestParametersPrimitivesController.class, "test_int")
                .append("/long", TestParametersPrimitivesController.class, "test_long")
                .append("/short", TestParametersPrimitivesController.class, "test_short")
                .append("/byte", TestParametersPrimitivesController.class, "test_byte")
                .append("/boolean", TestParametersPrimitivesController.class, "test_boolean")
                .append("/float", TestParametersPrimitivesController.class, "test_float")
                .append("/double", TestParametersPrimitivesController.class, "test_double")
                .append("/char", TestParametersPrimitivesController.class, "test_char")
        ;

        Assert.assertEquals("1", makeRequest("/int").param("v", 1).get().asString());
        Assert.assertEquals("1", makeRequest("/long").param("v", 1).get().asString());
        Assert.assertEquals("1", makeRequest("/short").param("v", 1).get().asString());
        Assert.assertEquals("1", makeRequest("/byte").param("v", 1).get().asString());
        Assert.assertEquals("1.0", makeRequest("/float").param("v", 1).get().asString());
        Assert.assertEquals("1.0", makeRequest("/double").param("v", 1).get().asString());
        Assert.assertEquals("true", makeRequest("/boolean").param("v", true).get().asString());
        Assert.assertEquals("a", makeRequest("/char").param("v", 'a').get().asString());

        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' it not present\"]}", makeRequest("/int").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/int").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' it not present\"]}", makeRequest("/long").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/long").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' it not present\"]}", makeRequest("/short").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/short").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' it not present\"]}", makeRequest("/byte").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/byte").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' it not present\"]}", makeRequest("/float").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/float").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' it not present\"]}", makeRequest("/double").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/double").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' it not present\"]}", makeRequest("/boolean").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' it not present\"]}", makeRequest("/char").get());
        checkResponse(400, "{\"messages\":[\"java.lang.IllegalArgumentException: Can't assign to char String with more then 1 character\"]}", makeRequest("/char").param("v", "abc").get());
    }
}
