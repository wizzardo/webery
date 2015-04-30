package com.wizzardo.http.framework.di;

import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.Handler;
import com.wizzardo.http.ServerTest;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.framework.template.Renderer;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @author: moxa
 * Date: 7/22/13
 */
public class TestDependencies extends ServerTest {

    @Injectable
    public static class A {
        B b;
    }

    @Injectable
    public static class B {
        A a;
    }

    @Injectable(scope = DependencyScope.PROTOTYPE)
    public static class C {
        A a;
    }

    @Injectable(scope = DependencyScope.SESSION)
    public static class D {
        A a;
        int counter = 0;
    }

    public static interface E {
        public String doIt();
    }

    @Injectable
    public static class F implements E {

        @Override
        public String doIt() {
            return "implementation";
        }
    }

    public static class J implements Service {

        public String doIt() {
            return "implementation";
        }
    }

    public static class IncrementHandler implements Handler{
        @Override
        public Response handle(Request request, Response response) throws IOException {
            return response;
        }
    }

    public static class SimplesController extends Controller {
        D d;
        E e;

        public Renderer increment() {
            d.counter++;
            return renderString(String.valueOf(d.counter));
        }

        public Renderer check() {
//            System.out.println(DependencyFactory.getDependency(F.class));
//            System.out.println(DependencyFactory.getDependency(E.class));

            return renderString(e.doIt());
        }
    }

    public static class SimplesController2 extends Controller {
        D d;

        public Renderer multiply() {
            d.counter *= 2;
            return renderString(String.valueOf(d.counter));
        }
    }

    public static class SimplesController3 extends Controller {
        J j;

        public Renderer service() {
            return renderString(j.doIt());
        }
    }

//    @Override
//    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
//        super.setUp();
//        server.getUrlMapping().append("/increment", SimplesController.class, "increment");
//        server.getUrlMapping().add("/multiply", SimplesController2.class, "multiply");
//        server.getUrlMapping().add("/interface", SimplesController.class, "check");
//        server.getUrlMapping().add("/service", SimplesController3.class, "service");
//    }

    @Test
    public void testCircularDependencies() {
        A a = DependencyFactory.getDependency(A.class);

        Assert.assertNotNull(a);
        Assert.assertNotNull(a.b);
        Assert.assertNotNull(a.b.a);


        B b = DependencyFactory.getDependency(B.class);

        Assert.assertNotNull(b);
        Assert.assertNotNull(b.a);
        Assert.assertNotNull(b.a.b);

        Assert.assertTrue(a.b == b);
        Assert.assertTrue(b.a == a);
    }

    @Test
    public void testSimplePrototype() {
        A a = DependencyFactory.getDependency(A.class);
        C c1 = DependencyFactory.getDependency(C.class);
        C c2 = DependencyFactory.getDependency(C.class);

        Assert.assertTrue(c1 != c2);
        Assert.assertTrue(c1.a == c2.a);
        Assert.assertTrue(c1.a == a);
    }

//    @Test
//    public void testSession() throws IOException {
//        List<Cookie> cookies = HttpClient.createRequest("http://localhost:8080/increment").get().getCookies();
//
//        Assert.assertEquals("2", HttpClient.createRequest("http://localhost:8080/increment").cookies(cookies).get().asString());
//        Assert.assertEquals("3", HttpClient.createRequest("http://localhost:8080/increment").cookies(cookies).get().asString());
//        Assert.assertEquals("1", HttpClient.createRequest("http://localhost:8080/increment").get().asString());
//        Assert.assertEquals("6", HttpClient.createRequest("http://localhost:8080/multiply").cookies(cookies).get().asString());
//    }

//    @Test
//    public void testInterface() throws IOException {
//        Assert.assertEquals("implementation", HttpClient.createRequest("http://localhost:8080/interface").get().asString());
//        J j = DependencyFactory.getDependency(J.class);
//        Assert.assertNotNull(j);
//        Assert.assertEquals("implementation", j.doIt());
//
//        SimplesController3 controller = DependencyFactory.getDependency(SimplesController3.class);
//        Assert.assertNotNull(controller.j);
//        Assert.assertEquals("implementation", controller.j.doIt());
//
//
////        Assert.assertEquals("implementation", HttpClient.connect("http://localhost:8080/service").get().asString());
//    }

}
