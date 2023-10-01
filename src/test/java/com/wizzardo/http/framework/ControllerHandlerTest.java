package com.wizzardo.http.framework;

import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.MultipartHandler;
import com.wizzardo.http.framework.parameters.Parameter;
import com.wizzardo.http.framework.parameters.ParametersHelper;
import com.wizzardo.http.framework.template.Model;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.request.Request;
import com.wizzardo.tools.evaluation.Config;
import com.wizzardo.tools.http.ContentType;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.json.JsonTools;
import com.wizzardo.tools.misc.With;
import com.wizzardo.tools.reflection.FieldReflectionFactory;
import com.wizzardo.tools.reflection.UnsafeTools;
import com.wizzardo.tools.security.MD5;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.wizzardo.http.request.Request.Method.GET;

/**
 * Created by wizzardo on 02.05.15.
 */
public class ControllerHandlerTest extends WebApplicationTest {

    @Override
    protected void configure(Config config) {
        config.config("server").config("multipart").put("enabled", true);
    }

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
        Assert.assertEquals("GET, HEAD, POST, PUT, DELETE, OPTIONS", makeRequest("/hello").options().header("Allow"));
    }

    @Test
    public void test_only_get() throws IOException {
        server.getUrlMapping()
                .append("/hello", HelloController.class, "hello", GET);

        Assert.assertEquals("hello!", makeRequest("/hello").get().asString());
        Assert.assertEquals("GET, HEAD, OPTIONS", makeRequest("/hello").options().header("Allow"));
        Assert.assertEquals(405, makeRequest("/hello").delete().getResponseCode());
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

        Assert.assertEquals("1", makeRequest("/int").param("v", 1).post().asString());
        Assert.assertEquals("1", makeRequest("/long").param("v", 1).post().asString());
        Assert.assertEquals("1", makeRequest("/short").param("v", 1).post().asString());
        Assert.assertEquals("1", makeRequest("/byte").param("v", 1).post().asString());
        Assert.assertEquals("1.0", makeRequest("/float").param("v", 1).post().asString());
        Assert.assertEquals("1.0", makeRequest("/double").param("v", 1).post().asString());
        Assert.assertEquals("true", makeRequest("/boolean").param("v", true).post().asString());
        Assert.assertEquals("a", makeRequest("/char").param("v", 'a').post().asString());

        Assert.assertEquals("1", makeRequest("/int").param("v", 1).addByteArray("data", new byte[0], "data").post().asString());
        Assert.assertEquals("1", makeRequest("/long").param("v", 1).addByteArray("data", new byte[0], "data").post().asString());
        Assert.assertEquals("1", makeRequest("/short").param("v", 1).addByteArray("data", new byte[0], "data").post().asString());
        Assert.assertEquals("1", makeRequest("/byte").param("v", 1).addByteArray("data", new byte[0], "data").post().asString());
        Assert.assertEquals("1.0", makeRequest("/float").param("v", 1).addByteArray("data", new byte[0], "data").post().asString());
        Assert.assertEquals("1.0", makeRequest("/double").param("v", 1).addByteArray("data", new byte[0], "data").post().asString());
        Assert.assertEquals("true", makeRequest("/boolean").param("v", true).addByteArray("data", new byte[0], "data").post().asString());
        Assert.assertEquals("a", makeRequest("/char").param("v", 'a').addByteArray("data", new byte[0], "data").post().asString());

        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' is not present\"]}", makeRequest("/int").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/int").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' is not present\"]}", makeRequest("/long").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/long").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' is not present\"]}", makeRequest("/short").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/short").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' is not present\"]}", makeRequest("/byte").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/byte").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' is not present\"]}", makeRequest("/float").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/float").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' is not present\"]}", makeRequest("/double").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NumberFormatException: For input string: \\\"a\\\"\"]}", makeRequest("/double").param("v", "a").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' is not present\"]}", makeRequest("/boolean").get());
        checkResponse(400, "{\"messages\":[\"java.lang.NullPointerException: parameter 'v' is not present\"]}", makeRequest("/char").get());
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

    public static class TestParametersPojo {
        @Parameter(def = "0")
        public int i;

        @Override
        public String toString() {
            return "i=" + i;
        }
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

        public Renderer test_bytes(@Parameter(name = "v") byte[] v) {
            return renderString(Arrays.toString(v));
        }

        public Renderer test_bytes_opt(@Parameter(name = "v") Optional<byte[]> v) {
            return renderString(v.map(Arrays::toString).orElse("default"));
        }

        public Renderer test_file(@Parameter(name = "v") File v) {
            return renderString(FileTools.text(v));
        }

        public Renderer test_file_opt(@Parameter(name = "v") Optional<File> v) {
            return renderString(v.map(FileTools::text).orElse("default"));
        }

        public Renderer test_pojo(@Parameter(name = "v") TestParametersPojo v) {
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
                .append("/bytes", controller, "test_bytes")
                .append("/bytes_opt", controller, "test_bytes_opt")
                .append("/file", controller, "test_file")
                .append("/file_opt", controller, "test_file_opt")
                .append("/pojo", controller, "test_pojo")
                .append("/double_multipart", new MultipartHandler(new ControllerHandler<>(controller, "test_bytes")))
        ;

        Assert.assertEquals("string", makeRequest("/string").param("v", "string").get().asString());
        Assert.assertEquals("null", makeRequest("/string").get().asString());
        Assert.assertEquals("string", makeRequest("/string_def").param("v", "string").get().asString());
        Assert.assertEquals("default", makeRequest("/string_def").get().asString());

        Assert.assertEquals("TWO", makeRequest("/enum").param("v", "TWO").get().asString());
        Assert.assertEquals("null", makeRequest("/enum").get().asString());
        Assert.assertEquals("TWO", makeRequest("/enum_def").param("v", "TWO").get().asString());
        Assert.assertEquals("ONE", makeRequest("/enum_def").get().asString());

        Assert.assertEquals("[1, 2, 3]", makeRequest("/bytes").addByteArray("v", new byte[]{1, 2, 3}, "v").post().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/file").addByteArray("v", "[1, 2, 3]".getBytes(), "v").post().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/bytes_opt").addByteArray("v", new byte[]{1, 2, 3}, "v").post().asString());
        Assert.assertEquals("[1, 2, 3]", makeRequest("/file_opt").addByteArray("v", "[1, 2, 3]".getBytes(), "v").post().asString());
        Assert.assertEquals("default", makeRequest("/bytes_opt").addByteArray("vv", new byte[]{1, 2, 3}, "vv").post().asString());
        Assert.assertEquals("default", makeRequest("/file_opt").addByteArray("vv", "[1, 2, 3]".getBytes(), "vv").post().asString());

        Assert.assertEquals("i=1", makeRequest("/pojo").param("i", "1").get().asString());
        Assert.assertEquals("i=0", makeRequest("/pojo").get().asString());
        Assert.assertEquals("i=1", makeRequest("/pojo").json(JsonTools.serialize(With.with(new TestParametersPojo(), it -> it.i = 1))).post().asString());

        Assert.assertEquals("[1, 2, 3]", makeRequest("/double_multipart").addByteArray("v", new byte[]{1, 2, 3}, "v").post().asString());
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

        public Renderer test_i(@Parameter(name = "v") Iterable<Integer> v) {
            return renderString((v == null ? null : v.getClass().getSimpleName()) + ": " + String.valueOf(v));
        }

        public Renderer test_i_def(@Parameter(name = "v", def = "4,5,6") Iterable<Integer> v) {
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
                .append("/i", controller, "test_i")
                .append("/i_def", controller, "test_i_def")
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

        Assert.assertEquals("null: null", makeRequest("/i").get().asString());
        Assert.assertEquals("ArrayList: [1, 2, 3]", makeRequest("/i").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("ArrayList: [1, 2, 3]", makeRequest("/i_def").param("v", 1).param("v", 2).param("v", 3).get().asString());
        Assert.assertEquals("ArrayList: [4, 5, 6]", makeRequest("/i_def").get().asString());
    }

    @Test
    public void test_parameter_has_name() throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
        Constructor<java.lang.reflect.Parameter> parameterConstructor = (Constructor<java.lang.reflect.Parameter>) java.lang.reflect.Parameter.class.getDeclaredConstructors()[0];
        parameterConstructor.setAccessible(true);

        Method method = (Method) UnsafeTools.getUnsafe().allocateInstance(Method.class);

        java.lang.reflect.Parameter parameter = parameterConstructor.newInstance("fieldName", 0, method, 0);

        new FieldReflectionFactory().create(Executable.class, "hasRealParameterData", true).setBoolean(method, true);
        new FieldReflectionFactory().create(Executable.class, "parameters", true).setObject(method, Array.newInstance(java.lang.reflect.Parameter.class, 1));

        Assert.assertEquals("fieldName", ParametersHelper.getParameterName(parameter));
    }

    static class MethodHolder {
        void method(Integer parameter) {
        }
    }

    @Test
    public void test_parameter_has_not_name() throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException, NoSuchMethodException {
        Method method = MethodHolder.class.getDeclaredMethod("method", Integer.class);
        Assert.assertEquals(null, ParametersHelper.getParameterName(method.getParameters()[0]));
    }

    public static class DefaultVariablesController extends Controller {
        public Renderer test_1() {
            response.header("foo", "bar");
            return renderView();
        }
    }

    @Test
    public void test_request_in_view() throws IOException {
        server.getUrlMapping().append("/variables", DefaultVariablesController.class, "test_1");
        Assert.assertEquals("\n" +
                        "/variables\n" +
                        "bar\n" +
                        "defaultVariables\n" +
                        "test_1\n" +
                        "defaultVariables.test_1"
                , makeRequest("/variables").get().asString());
    }

    public static class CustomTypesController extends Controller {
        public String string() {
            return "foo";
        }

        public byte[] bytes() {
            return "foo".getBytes(StandardCharsets.UTF_8);
        }

        public ReadableData renderable() {
            return new ReadableByteArray("foo".getBytes(StandardCharsets.UTF_8));
        }

        static class JsonOutput {
            String foo = "bar";
        }

        public JsonOutput json() {
            return new JsonOutput();
        }

        public Model view() {
            return model();
        }

        public void nothing() {
        }

        public int integer() {
            return -1;
        }

        public Character character() {
            return 'A';
        }
    }

    @Test
    public void test_custom_types() throws IOException {
        server.getUrlMapping()
                .append("/string", CustomTypesController.class, "string")
                .append("/bytes", CustomTypesController.class, "bytes")
                .append("/renderable", CustomTypesController.class, "renderable")
                .append("/json", CustomTypesController.class, "json")
                .append("/view", CustomTypesController.class, "view")
                .append("/nothing", CustomTypesController.class, "nothing")
        ;

        Assert.assertEquals("foo", makeRequest("/string").get().asString());
        Assert.assertEquals("foo", makeRequest("/bytes").get().asString());
        Assert.assertEquals("foo", makeRequest("/renderable").get().asString());
        Assert.assertEquals("foo", makeRequest("/view").get().asString().trim());
        Assert.assertEquals("", makeRequest("/nothing").get().asString().trim());
        Assert.assertEquals("{\"foo\":\"bar\"}", makeRequest("/json").get().asString());

        checkException(() -> server.getUrlMapping().append("/integer", CustomTypesController.class, "integer"), IllegalStateException.class, "Cannot create renderer for int");
        checkException(() -> server.getUrlMapping().append("/character", CustomTypesController.class, "character"), IllegalStateException.class, "Cannot create renderer for class java.lang.Character");
    }


    public static class TestMultipartController extends Controller {

        public static class TestDataInfo {
            String name;
        }

        public String test(
                @Parameter(name = "data") byte[] data,
                @Parameter(name = "info") TestDataInfo info
        ) {
            return info.name + ":" + MD5.create().update(data).asString();
        }
    }

    @Test
    public void testJsonAndBinaryParams() throws IOException {
        byte[] data = new byte[10 * 1024 * 1024];
        new Random().nextBytes(data);
        final String md5 = MD5.create().update(data).asString();

        server.getUrlMapping()
                .append("/", TestMultipartController.class, "test", Request.Method.POST);

        String name = "test-name";

        TestMultipartController.TestDataInfo testDataInfo = new TestMultipartController.TestDataInfo();
        testDataInfo.name = name;

        String responseString = makeRequest("/")
                .addByteArray("info", JsonTools.serializeToBytes(testDataInfo), "info", ContentType.JSON.value)
                .addByteArray("data", data, "just some data")
                .post().asString();

        Assert.assertEquals(name + ":" + md5, responseString);
    }
}
