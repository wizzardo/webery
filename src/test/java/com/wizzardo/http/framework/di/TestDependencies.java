package com.wizzardo.http.framework.di;

import com.wizzardo.http.Handler;
import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.ControllerHandler;
import com.wizzardo.http.framework.WebApplicationTest;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.http.HttpSession;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @author: moxa
 * Date: 7/22/13
 */
public class TestDependencies extends WebApplicationTest {

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

    public interface E {
        default String doIt() {
            return getClass().getSimpleName();
        }
    }

    @Injectable
    public static class F implements E {
    }

    public static class J implements Service, E {
    }

    public static class IncrementHandler implements Handler {
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

    @Override
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        super.setUp();
        server.getUrlMapping()
                .append("/interface", new ControllerHandler(SimplesController.class, "check"))
                .append("/increment", new ControllerHandler(SimplesController.class, "increment"))
                .append("/multiply", new ControllerHandler(SimplesController2.class, "multiply"))
                .append("/service", new ControllerHandler(SimplesController3.class, "service"))
        ;
    }

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

    @Test
    public void testSession() throws IOException {
        Assert.assertEquals("1", makeRequest("/increment").get().asString());
        Assert.assertEquals("2", makeRequest("/increment").get().asString());
        Assert.assertEquals("1", makeRequest("/increment", new HttpSession()).get().asString());
        Assert.assertEquals("4", makeRequest("/multiply").get().asString());
    }

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
