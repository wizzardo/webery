package com.wizzardo.http.framework;

import com.wizzardo.http.framework.template.Renderer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

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
    public void test_parameters_1() throws IOException {
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
    public void test_parameters_primitives() throws IOException {
        Class<? extends Controller> controller = TestParametersPrimitivesController.class;
        server.getUrlMapping()
                .append("/int", controller, "test_int")
                .append("/long", controller, "test_long")
                .append("/short", controller, "test_short")
                .append("/byte", controller, "test_byte")
                .append("/boolean", controller, "test_boolean")
                .append("/float", controller, "test_float")
                .append("/double", controller, "test_double")
                .append("/char", controller, "test_char")
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

    public static class TestParametersPrimitivesWithDefaultsController extends Controller {
        public Renderer test_int(@Parameter(name = "v", def = "1") int v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_long(@Parameter(name = "v", def = "1") long v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_short(@Parameter(name = "v", def = "1") short v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_byte(@Parameter(name = "v", def = "1") byte v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_boolean(@Parameter(name = "v", def = "true") boolean v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_float(@Parameter(name = "v", def = "1") float v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_double(@Parameter(name = "v", def = "1") double v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_char(@Parameter(name = "v", def = "a") char v) {
            return renderString(String.valueOf(v));
        }
    }

    @Test
    public void test_parameters_primitives_with_defaults() throws IOException {
        Class<? extends Controller> controller = TestParametersPrimitivesWithDefaultsController.class;
        server.getUrlMapping()
                .append("/int", controller, "test_int")
                .append("/long", controller, "test_long")
                .append("/short", controller, "test_short")
                .append("/byte", controller, "test_byte")
                .append("/boolean", controller, "test_boolean")
                .append("/float", controller, "test_float")
                .append("/double", controller, "test_double")
                .append("/char", controller, "test_char")
        ;

        Assert.assertEquals("1", makeRequest("/int").get().asString());
        Assert.assertEquals("1", makeRequest("/long").get().asString());
        Assert.assertEquals("1", makeRequest("/short").get().asString());
        Assert.assertEquals("1", makeRequest("/byte").get().asString());
        Assert.assertEquals("1.0", makeRequest("/float").get().asString());
        Assert.assertEquals("1.0", makeRequest("/double").get().asString());
        Assert.assertEquals("true", makeRequest("/boolean").get().asString());
        Assert.assertEquals("a", makeRequest("/char").get().asString());

        Assert.assertEquals("2", makeRequest("/int").param("v", 2).get().asString());
        Assert.assertEquals("2", makeRequest("/long").param("v", 2).get().asString());
        Assert.assertEquals("2", makeRequest("/short").param("v", 2).get().asString());
        Assert.assertEquals("2", makeRequest("/byte").param("v", 2).get().asString());
        Assert.assertEquals("2.0", makeRequest("/float").param("v", 2).get().asString());
        Assert.assertEquals("2.0", makeRequest("/double").param("v", 2).get().asString());
        Assert.assertEquals("false", makeRequest("/boolean").param("v", false).get().asString());
        Assert.assertEquals("b", makeRequest("/char").param("v", 'b').get().asString());
    }


    public static class TestParametersBoxedController extends Controller {
        public Renderer test_int(@Parameter(name = "v") Integer v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_long(@Parameter(name = "v") Long v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_short(@Parameter(name = "v") Short v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_byte(@Parameter(name = "v") Byte v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_boolean(@Parameter(name = "v") Boolean v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_float(@Parameter(name = "v") Float v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_double(@Parameter(name = "v") Double v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_char(@Parameter(name = "v") Character v) {
            return renderString(String.valueOf(v));
        }
    }

    @Test
    public void test_parameters_boxed() throws IOException {
        Class<? extends Controller> controller = TestParametersBoxedController.class;
        server.getUrlMapping()
                .append("/int", controller, "test_int")
                .append("/long", controller, "test_long")
                .append("/short", controller, "test_short")
                .append("/byte", controller, "test_byte")
                .append("/boolean", controller, "test_boolean")
                .append("/float", controller, "test_float")
                .append("/double", controller, "test_double")
                .append("/char", controller, "test_char")
        ;

        Assert.assertEquals("1", makeRequest("/int").param("v", 1).get().asString());
        Assert.assertEquals("1", makeRequest("/long").param("v", 1).get().asString());
        Assert.assertEquals("1", makeRequest("/short").param("v", 1).get().asString());
        Assert.assertEquals("1", makeRequest("/byte").param("v", 1).get().asString());
        Assert.assertEquals("1.0", makeRequest("/float").param("v", 1).get().asString());
        Assert.assertEquals("1.0", makeRequest("/double").param("v", 1).get().asString());
        Assert.assertEquals("true", makeRequest("/boolean").param("v", true).get().asString());
        Assert.assertEquals("a", makeRequest("/char").param("v", 'a').get().asString());

        checkResponse(200, "null", makeRequest("/int").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/int").param("v", "a").get());
        checkResponse(200, "null", makeRequest("/long").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/long").param("v", "a").get());
        checkResponse(200, "null", makeRequest("/short").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/short").param("v", "a").get());
        checkResponse(200, "null", makeRequest("/byte").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/byte").param("v", "a").get());
        checkResponse(200, "null", makeRequest("/float").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/float").param("v", "a").get());
        checkResponse(200, "null", makeRequest("/double").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/double").param("v", "a").get());
        checkResponse(200, "null", makeRequest("/boolean").get());
        checkResponse(200, "null", makeRequest("/char").get());
        checkResponse(400, "{\"messages\":[\"java.lang.IllegalArgumentException: Can't assign to char String with more then 1 character\"]}", makeRequest("/char").param("v", "abc").get());
    }


    public static class TestParametersBoxedWithDefaultsController extends Controller {
        public Renderer test_int(@Parameter(name = "v", def = "1") Integer v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_long(@Parameter(name = "v", def = "1") Long v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_short(@Parameter(name = "v", def = "1") Short v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_byte(@Parameter(name = "v", def = "1") Byte v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_boolean(@Parameter(name = "v", def = "true") Boolean v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_float(@Parameter(name = "v", def = "1") Float v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_double(@Parameter(name = "v", def = "1") Double v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_char(@Parameter(name = "v", def = "a") Character v) {
            return renderString(String.valueOf(v));
        }
    }

    @Test
    public void test_parameters_boxed_with_defaults() throws IOException {
        Class<? extends Controller> controller = TestParametersBoxedWithDefaultsController.class;
        server.getUrlMapping()
                .append("/int", controller, "test_int")
                .append("/long", controller, "test_long")
                .append("/short", controller, "test_short")
                .append("/byte", controller, "test_byte")
                .append("/boolean", controller, "test_boolean")
                .append("/float", controller, "test_float")
                .append("/double", controller, "test_double")
                .append("/char", controller, "test_char")
        ;

        Assert.assertEquals("1", makeRequest("/int").get().asString());
        Assert.assertEquals("1", makeRequest("/long").get().asString());
        Assert.assertEquals("1", makeRequest("/short").get().asString());
        Assert.assertEquals("1", makeRequest("/byte").get().asString());
        Assert.assertEquals("1.0", makeRequest("/float").get().asString());
        Assert.assertEquals("1.0", makeRequest("/double").get().asString());
        Assert.assertEquals("true", makeRequest("/boolean").get().asString());
        Assert.assertEquals("a", makeRequest("/char").get().asString());

        Assert.assertEquals("2", makeRequest("/int").param("v", 2).get().asString());
        Assert.assertEquals("2", makeRequest("/long").param("v", 2).get().asString());
        Assert.assertEquals("2", makeRequest("/short").param("v", 2).get().asString());
        Assert.assertEquals("2", makeRequest("/byte").param("v", 2).get().asString());
        Assert.assertEquals("2.0", makeRequest("/float").param("v", 2).get().asString());
        Assert.assertEquals("2.0", makeRequest("/double").param("v", 2).get().asString());
        Assert.assertEquals("false", makeRequest("/boolean").param("v", false).get().asString());
        Assert.assertEquals("b", makeRequest("/char").param("v", 'b').get().asString());
    }


    public enum TestParameterEnum {
        ONE, TWO, THREE;
    }

    public static class TestParametersOtherController extends Controller {
        public Renderer test_string(@Parameter(name = "v") String v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_string_def(@Parameter(name = "v", def = "default") String v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_enum(@Parameter(name = "v") TestParameterEnum v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_enum_def(@Parameter(name = "v", def = "ONE") TestParameterEnum v) {
            return renderString(String.valueOf(v));
        }
    }

    @Test
    public void test_parameters_other() throws IOException {
        Class<? extends Controller> controller = TestParametersOtherController.class;
        server.getUrlMapping()
                .append("/string", controller, "test_string")
                .append("/string_def", controller, "test_string_def")
                .append("/enum", controller, "test_enum")
                .append("/enum_def", controller, "test_enum_def")
        ;

        Assert.assertEquals("string", makeRequest("/string").param("v", "string").get().asString());
        Assert.assertEquals("null", makeRequest("/string").get().asString());
        Assert.assertEquals("string", makeRequest("/string_def").param("v", "string").get().asString());
        Assert.assertEquals("default", makeRequest("/string_def").get().asString());

        Assert.assertEquals("TWO", makeRequest("/enum").param("v", "TWO").get().asString());
        Assert.assertEquals("null", makeRequest("/enum").get().asString());
        Assert.assertEquals("TWO", makeRequest("/enum_def").param("v", "TWO").get().asString());
        Assert.assertEquals("ONE", makeRequest("/enum_def").get().asString());
    }


    public static class TestParametersArrayPrimitivesController extends Controller {
        public Renderer test_int(@Parameter(name = "v") int[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_long(@Parameter(name = "v") long[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_short(@Parameter(name = "v") short[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_byte(@Parameter(name = "v") byte[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_boolean(@Parameter(name = "v") boolean[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_float(@Parameter(name = "v") float[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_double(@Parameter(name = "v") double[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_char(@Parameter(name = "v") char[] v) {
            return renderString(Arrays.toString(v));
        }
    }

    @Test
    public void test_parameters_array_primitives() throws IOException {
        Class<? extends Controller> controller = TestParametersArrayPrimitivesController.class;
        server.getUrlMapping()
                .append("/int", controller, "test_int")
                .append("/long", controller, "test_long")
                .append("/short", controller, "test_short")
                .append("/byte", controller, "test_byte")
                .append("/boolean", controller, "test_boolean")
                .append("/float", controller, "test_float")
                .append("/double", controller, "test_double")
                .append("/char", controller, "test_char")
        ;

        Assert.assertEquals("[1, 2, 3]", makeRequest("/int").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/long").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/short").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/byte").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/float").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/double").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[true, false, true]", makeRequest("/boolean").param("v", true).param("v", false).param("v", true).get().asString());
        Assert.assertEquals("[a, b, c]", makeRequest("/char").param("v", 'a').param("v", 'b').param("v", 'c').get().asString());
    }


    public static class TestParametersArrayPrimitivesWithDefaultsController extends Controller {
        public Renderer test_int(@Parameter(name = "v", def = "1,2,3") int[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_long(@Parameter(name = "v", def = "1,2,3") long[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_short(@Parameter(name = "v", def = "1,2,3") short[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_byte(@Parameter(name = "v", def = "1,2,3") byte[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_boolean(@Parameter(name = "v", def = "true,false,true") boolean[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_float(@Parameter(name = "v", def = "1,2,3") float[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_double(@Parameter(name = "v", def = "1,2,3") double[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_char(@Parameter(name = "v", def = "a,b,c") char[] v) {
            return renderString(Arrays.toString(v));
        }
    }

    @Test
    public void test_parameters_array_primitives_with_defaults() throws IOException {
        Class<? extends Controller> controller = TestParametersArrayPrimitivesWithDefaultsController.class;
        server.getUrlMapping()
                .append("/int", controller, "test_int")
                .append("/long", controller, "test_long")
                .append("/short", controller, "test_short")
                .append("/byte", controller, "test_byte")
                .append("/boolean", controller, "test_boolean")
                .append("/float", controller, "test_float")
                .append("/double", controller, "test_double")
                .append("/char", controller, "test_char")
        ;

        Assert.assertEquals("[1, 2, 3]", makeRequest("/int").get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/long").get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/short").get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/byte").get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/float").get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/double").get().asString());
        Assert.assertEquals("[true, false, true]", makeRequest("/boolean").get().asString());
        Assert.assertEquals("[a, b, c]", makeRequest("/char").get().asString());

        Assert.assertEquals("[4, 5, 6]", makeRequest("/int").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4, 5, 6]", makeRequest("/long").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4, 5, 6]", makeRequest("/short").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4, 5, 6]", makeRequest("/byte").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4.0, 5.0, 6.0]", makeRequest("/float").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4.0, 5.0, 6.0]", makeRequest("/double").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[false, true, false]", makeRequest("/boolean").param("v", false).param("v", true).param("v", false).get().asString());
        Assert.assertEquals("[d, e, f]", makeRequest("/char").param("v", 'd').param("v", 'e').param("v", 'f').get().asString());
    }


    public static class TestParametersArrayBoxedController extends Controller {
        public Renderer test_int(@Parameter(name = "v") Integer[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_long(@Parameter(name = "v") Long[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_short(@Parameter(name = "v") Short[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_byte(@Parameter(name = "v") Byte[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_boolean(@Parameter(name = "v") Boolean[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_float(@Parameter(name = "v") Float[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_double(@Parameter(name = "v") Double[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_char(@Parameter(name = "v") Character[] v) {
            return renderString(Arrays.toString(v));
        }
    }

    @Test
    public void test_parameters_array_boxed() throws IOException {
        Class<? extends Controller> controller = TestParametersArrayBoxedController.class;
        server.getUrlMapping()
                .append("/int", controller, "test_int")
                .append("/long", controller, "test_long")
                .append("/short", controller, "test_short")
                .append("/byte", controller, "test_byte")
                .append("/boolean", controller, "test_boolean")
                .append("/float", controller, "test_float")
                .append("/double", controller, "test_double")
                .append("/char", controller, "test_char")
        ;

        Assert.assertEquals("[1, 2, 3]", makeRequest("/int").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/long").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/short").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/byte").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/float").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/double").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[true, false, true]", makeRequest("/boolean").param("v", true).param("v", false).param("v", true).get().asString());
        Assert.assertEquals("[a, b, c]", makeRequest("/char").param("v", 'a').param("v", 'b').param("v", 'c').get().asString());
    }


    public static class TestParametersArrayBoxedWithDefaultsController extends Controller {
        public Renderer test_int(@Parameter(name = "v", def = "1,2,3") Integer[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_long(@Parameter(name = "v", def = "1,2,3") Long[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_short(@Parameter(name = "v", def = "1,2,3") Short[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_byte(@Parameter(name = "v", def = "1,2,3") Byte[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_boolean(@Parameter(name = "v", def = "true,false,true") Boolean[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_float(@Parameter(name = "v", def = "1,2,3") Float[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_double(@Parameter(name = "v", def = "1,2,3") Double[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_char(@Parameter(name = "v", def = "a,b,c") Character[] v) {
            return renderString(Arrays.toString(v));
        }
    }

    @Test
    public void test_parameters_array_boxed_with_defaults() throws IOException {
        Class<? extends Controller> controller = TestParametersArrayBoxedWithDefaultsController.class;
        server.getUrlMapping()
                .append("/int", controller, "test_int")
                .append("/long", controller, "test_long")
                .append("/short", controller, "test_short")
                .append("/byte", controller, "test_byte")
                .append("/boolean", controller, "test_boolean")
                .append("/float", controller, "test_float")
                .append("/double", controller, "test_double")
                .append("/char", controller, "test_char")
        ;

        Assert.assertEquals("[1, 2, 3]", makeRequest("/int").get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/long").get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/short").get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/byte").get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/float").get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/double").get().asString());
        Assert.assertEquals("[true, false, true]", makeRequest("/boolean").get().asString());
        Assert.assertEquals("[a, b, c]", makeRequest("/char").get().asString());

        Assert.assertEquals("[4, 5, 6]", makeRequest("/int").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4, 5, 6]", makeRequest("/long").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4, 5, 6]", makeRequest("/short").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4, 5, 6]", makeRequest("/byte").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4.0, 5.0, 6.0]", makeRequest("/float").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4.0, 5.0, 6.0]", makeRequest("/double").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[false, true, false]", makeRequest("/boolean").param("v", false).param("v", true).param("v", false).get().asString());
        Assert.assertEquals("[d, e, f]", makeRequest("/char").param("v", 'd').param("v", 'e').param("v", 'f').get().asString());
    }


    public static class TestParametersArrayOtherController extends Controller {
        public Renderer test_string(@Parameter(name = "v") String[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_string_def(@Parameter(name = "v", def = "a,b,c") String[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_enum(@Parameter(name = "v") TestParameterEnum[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_enum_def(@Parameter(name = "v", def = "ONE,TWO") TestParameterEnum[] v) {
            return renderString(Arrays.toString(v));
        }
    }

    @Test
    public void test_parameters_array_other() throws IOException {
        Class<? extends Controller> controller = TestParametersArrayOtherController.class;
        server.getUrlMapping()
                .append("/string", controller, "test_string")
                .append("/string_def", controller, "test_string_def")
                .append("/enum", controller, "test_enum")
                .append("/enum_def", controller, "test_enum_def")
        ;

        Assert.assertEquals("[a, b, c]", makeRequest("/string").param("v", "a").param("v", "b").param("v", "c").get().asString());
        Assert.assertEquals("null", makeRequest("/string").get().asString());
        Assert.assertEquals("[d, e, f]", makeRequest("/string_def").param("v", "d").param("v", "e").param("v", "f").get().asString());
        Assert.assertEquals("[a, b, c]", makeRequest("/string_def").get().asString());

        Assert.assertEquals("[ONE, TWO, THREE]", makeRequest("/enum").param("v", "ONE").param("v", "TWO").param("v", "THREE").get().asString());
        Assert.assertEquals("null", makeRequest("/enum").get().asString());
        Assert.assertEquals("[ONE, TWO, THREE]", makeRequest("/enum_def").param("v", "ONE").param("v", "TWO").param("v", "THREE").get().asString());
        Assert.assertEquals("[ONE, TWO]", makeRequest("/enum_def").get().asString());
    }


    public static class TestParametersCollectionBoxedController extends Controller {
        public Renderer test_int(@Parameter(name = "v") List<Integer> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_long(@Parameter(name = "v") List<Long> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_short(@Parameter(name = "v") List<Short> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_byte(@Parameter(name = "v") List<Byte> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_boolean(@Parameter(name = "v") List<Boolean> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_float(@Parameter(name = "v") List<Float> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_double(@Parameter(name = "v") List<Double> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_char(@Parameter(name = "v") List<Character> v) {
            return renderString(String.valueOf(v));
        }
    }

    @Test
    public void test_parameters_collection() throws IOException {
        Class<? extends Controller> controller = TestParametersCollectionBoxedController.class;
        server.getUrlMapping()
                .append("/int", controller, "test_int")
                .append("/long", controller, "test_long")
                .append("/short", controller, "test_short")
                .append("/byte", controller, "test_byte")
                .append("/boolean", controller, "test_boolean")
                .append("/float", controller, "test_float")
                .append("/double", controller, "test_double")
                .append("/char", controller, "test_char")
        ;

        Assert.assertEquals("[1, 2, 3]", makeRequest("/int").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/long").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/short").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/byte").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/float").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/double").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("[true, false, true]", makeRequest("/boolean").param("v", true).param("v", false).param("v", true).get().asString());
        Assert.assertEquals("[a, b, c]", makeRequest("/char").param("v", 'a').param("v", 'b').param("v", 'c').get().asString());
    }


    public static class TestParametersCollectionBoxedWithDefaultsController extends Controller {
        public Renderer test_int(@Parameter(name = "v", def = "1,2,3") List<Integer> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_long(@Parameter(name = "v", def = "1,2,3") List<Long> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_short(@Parameter(name = "v", def = "1,2,3") List<Short> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_byte(@Parameter(name = "v", def = "1,2,3") List<Byte> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_boolean(@Parameter(name = "v", def = "true,false,true") List<Boolean> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_float(@Parameter(name = "v", def = "1,2,3") List<Float> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_double(@Parameter(name = "v", def = "1,2,3") List<Double> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_char(@Parameter(name = "v", def = "a,b,c") List<Character> v) {
            return renderString(String.valueOf(v));
        }
    }

    @Test
    public void test_parameters_collection_boxed_with_defaults() throws IOException {
        Class<? extends Controller> controller = TestParametersCollectionBoxedWithDefaultsController.class;
        server.getUrlMapping()
                .append("/int", controller, "test_int")
                .append("/long", controller, "test_long")
                .append("/short", controller, "test_short")
                .append("/byte", controller, "test_byte")
                .append("/boolean", controller, "test_boolean")
                .append("/float", controller, "test_float")
                .append("/double", controller, "test_double")
                .append("/char", controller, "test_char")
        ;

        Assert.assertEquals("[1, 2, 3]", makeRequest("/int").get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/long").get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/short").get().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/byte").get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/float").get().asString());
        Assert.assertEquals("[1.0, 2.0, 3.0]", makeRequest("/double").get().asString());
        Assert.assertEquals("[true, false, true]", makeRequest("/boolean").get().asString());
        Assert.assertEquals("[a, b, c]", makeRequest("/char").get().asString());

        Assert.assertEquals("[4, 5, 6]", makeRequest("/int").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4, 5, 6]", makeRequest("/long").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4, 5, 6]", makeRequest("/short").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4, 5, 6]", makeRequest("/byte").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4.0, 5.0, 6.0]", makeRequest("/float").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[4.0, 5.0, 6.0]", makeRequest("/double").param("v", 4).param("v", 5).param("v", 6).get().asString());
        Assert.assertEquals("[false, true, false]", makeRequest("/boolean").param("v", false).param("v", true).param("v", false).get().asString());
        Assert.assertEquals("[d, e, f]", makeRequest("/char").param("v", 'd').param("v", 'e').param("v", 'f').get().asString());
    }


    public static class TestParametersCollectionOtherController extends Controller {
        public Renderer test_string(@Parameter(name = "v") List<String> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_string_def(@Parameter(name = "v", def = "a,b,c") List<String> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_enum(@Parameter(name = "v") List<TestParameterEnum> v) {
            return renderString(String.valueOf(v));
        }

        public Renderer test_enum_def(@Parameter(name = "v", def = "ONE,TWO") List<TestParameterEnum> v) {
            return renderString(String.valueOf(v));
        }
    }

    @Test
    public void test_parameters_collection_other() throws IOException {
        Class<? extends Controller> controller = TestParametersCollectionOtherController.class;
        server.getUrlMapping()
                .append("/string", controller, "test_string")
                .append("/string_def", controller, "test_string_def")
                .append("/enum", controller, "test_enum")
                .append("/enum_def", controller, "test_enum_def")
        ;

        Assert.assertEquals("[a, b, c]", makeRequest("/string").param("v", "a").param("v", "b").param("v", "c").get().asString());
        Assert.assertEquals("null", makeRequest("/string").get().asString());
        Assert.assertEquals("[d, e, f]", makeRequest("/string_def").param("v", "d").param("v", "e").param("v", "f").get().asString());
        Assert.assertEquals("[a, b, c]", makeRequest("/string_def").get().asString());

        Assert.assertEquals("[ONE, TWO, THREE]", makeRequest("/enum").param("v", "ONE").param("v", "TWO").param("v", "THREE").get().asString());
        Assert.assertEquals("null", makeRequest("/enum").get().asString());
        Assert.assertEquals("[ONE, TWO, THREE]", makeRequest("/enum_def").param("v", "ONE").param("v", "TWO").param("v", "THREE").get().asString());
        Assert.assertEquals("[ONE, TWO]", makeRequest("/enum_def").get().asString());
    }


    public static class TestParametersDifferentCollectionsController extends Controller {
        public Renderer test_list(@Parameter(name = "v") List<Integer> v) {
            return renderString((v == null ? null : v.getClass().getSimpleName()) + ": " + String.valueOf(v));
        }

        public Renderer test_list_def(@Parameter(name = "v", def = "4,5,6") List<Integer> v) {
            return renderString(v.getClass().getSimpleName() + ": " + String.valueOf(v));
        }

        public Renderer test_set(@Parameter(name = "v") Set<Integer> v) {
            return renderString((v == null ? null : v.getClass().getSimpleName()) + ": " + String.valueOf(v));
        }

        public Renderer test_set_def(@Parameter(name = "v", def = "4,5,6") Set<Integer> v) {
            return renderString(v.getClass().getSimpleName() + ": " + String.valueOf(v));
        }

        public Renderer test_ll(@Parameter(name = "v") LinkedList<Integer> v) {
            return renderString((v == null ? null : v.getClass().getSimpleName()) + ": " + String.valueOf(v));
        }

        public Renderer test_ll_def(@Parameter(name = "v", def = "4,5,6") LinkedList<Integer> v) {
            return renderString(v.getClass().getSimpleName() + ": " + String.valueOf(v));
        }

        public Renderer test_ts(@Parameter(name = "v") TreeSet<Integer> v) {
            return renderString((v == null ? null : v.getClass().getSimpleName()) + ": " + String.valueOf(v));
        }

        public Renderer test_ts_def(@Parameter(name = "v", def = "4,5,6") TreeSet<Integer> v) {
            return renderString(v.getClass().getSimpleName() + ": " + String.valueOf(v));
        }
    }

    @Test
    public void test_parameters_different_collections() throws IOException {
        Class<? extends Controller> controller = TestParametersDifferentCollectionsController.class;
        server.getUrlMapping()
                .append("/list", controller, "test_list")
                .append("/list_def", controller, "test_list_def")
                .append("/set", controller, "test_set")
                .append("/set_def", controller, "test_set_def")
                .append("/ll", controller, "test_ll")
                .append("/ll_def", controller, "test_ll_def")
                .append("/ts", controller, "test_ts")
                .append("/ts_def", controller, "test_ts_def")
        ;

        Assert.assertEquals("null: null", makeRequest("/list").get().asString());
        Assert.assertEquals("ArrayList: [1, 2, 3]", makeRequest("/list").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("ArrayList: [1, 2, 3]", makeRequest("/list_def").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("ArrayList: [4, 5, 6]", makeRequest("/list_def").get().asString());

        Assert.assertEquals("null: null", makeRequest("/set").get().asString());
        Assert.assertEquals("HashSet: [1, 2, 3]", makeRequest("/set").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("HashSet: [1, 2, 3]", makeRequest("/set_def").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("HashSet: [4, 5, 6]", makeRequest("/set_def").get().asString());

        Assert.assertEquals("null: null", makeRequest("/ll").get().asString());
        Assert.assertEquals("LinkedList: [1, 2, 3]", makeRequest("/ll").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("LinkedList: [1, 2, 3]", makeRequest("/ll_def").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("LinkedList: [4, 5, 6]", makeRequest("/ll_def").get().asString());

        Assert.assertEquals("null: null", makeRequest("/ts").get().asString());
        Assert.assertEquals("TreeSet: [1, 2, 3]", makeRequest("/ts").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("TreeSet: [1, 2, 3]", makeRequest("/ts_def").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("TreeSet: [4, 5, 6]", makeRequest("/ts_def").get().asString());
    }
}
