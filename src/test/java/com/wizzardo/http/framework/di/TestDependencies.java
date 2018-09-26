package com.wizzardo.http.framework.di;

import com.wizzardo.http.Handler;
import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.WebApplicationTest;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.http.HttpSession;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

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
        int counter = 0;
    }

    public interface E {
        default String doIt() {
            return getClass().getSimpleName();
        }
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
//            System.out.println(DependencyFactory.get(F.class));
//            System.out.println(DependencyFactory.get(E.class));

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
                .append("/interface", SimplesController.class, "check")
                .append("/increment", SimplesController.class, "increment")
                .append("/multiply", SimplesController2.class, "multiply")
                .append("/service", SimplesController3.class, "service")
                .append("/request_scope", RequestScopeController.class, "check")
        ;
    }

    static class Holder {
    }

    static class HolderService implements Service {
        Holder holder;
    }

    @Test
    public void testRegisterManually() {
        Holder holder = new Holder();

        DependencyFactory.get().register(Holder.class, new SingletonDependency<>(holder));

        Holder test = DependencyFactory.get(Holder.class);
        Assert.assertNotNull(test);
        Assert.assertSame(holder, test);

        test = DependencyFactory.get(HolderService.class).holder;
        Assert.assertNotNull(test);
        Assert.assertSame(holder, test);
    }

    public interface SomeInterface {
    }

    public static class SomeImplementation1 implements SomeInterface, Service {
    }

    public static class SomeImplementation2 implements SomeInterface, Service {
    }

    @Test
    public void testRegisterManually_2() {
        DependencyFactory.get().register(SomeInterface.class, SomeImplementation1.class);

        SomeInterface test = DependencyFactory.get(SomeInterface.class);
        Assert.assertNotNull(test);
        Assert.assertTrue(test instanceof SomeImplementation1);
    }

    public static class InjectableFooService implements Service {
    }

    public static class MockedFooService extends InjectableFooService {
    }

    @Test
    public void testRegisterManually_3() {
        DependencyFactory.get().register(InjectableFooService.class, MockedFooService.class);

        InjectableFooService test = DependencyFactory.get(InjectableFooService.class);
        Assert.assertNotNull(test);
        Assert.assertTrue(test instanceof MockedFooService);
    }

    @Test
    public void testCircularDependencies() {
        A a = DependencyFactory.get(A.class);

        Assert.assertNotNull(a);
        Assert.assertNotNull(a.b);
        Assert.assertNotNull(a.b.a);


        B b = DependencyFactory.get(B.class);

        Assert.assertNotNull(b);
        Assert.assertNotNull(b.a);
        Assert.assertNotNull(b.a.b);

        Assert.assertTrue(a.b == b);
        Assert.assertTrue(b.a == a);
    }

    @Test
    public void testSimplePrototype() {
        A a = DependencyFactory.get(A.class);
        C c1 = DependencyFactory.get(C.class);
        C c2 = DependencyFactory.get(C.class);

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

    @Test
    public void testInterface() throws IOException {
        Assert.assertEquals("J", makeRequest("/interface").get().asString());
        J j = DependencyFactory.get(J.class);
        Assert.assertNotNull(j);
        Assert.assertEquals("J", j.doIt());

        SimplesController3 controller = DependencyFactory.get(SimplesController3.class);
        Assert.assertNotNull(controller.j);
        Assert.assertEquals("J", controller.j.doIt());


        Assert.assertEquals("J", makeRequest("/service").get().asString());
    }

    @Injectable(scope = DependencyScope.THREAD_LOCAL)
    public static class Counter {
        int value;
    }

    @Test
    public void testThreadLocal() throws InterruptedException {
        Counter counter = DependencyFactory.get(Counter.class);
        Assert.assertEquals(0, counter.value);

        counter.value++;
        Assert.assertEquals(1, counter.value);
        Assert.assertEquals(1, DependencyFactory.get(Counter.class).value);
        Thread thread = new Thread(() -> {
            Assert.assertEquals(0, DependencyFactory.get(Counter.class).value);
        });
        thread.start();
        thread.join();

        Assert.assertEquals(1, DependencyFactory.get(Counter.class).value);
    }

    @Injectable(scope = DependencyScope.REQUEST)
    public static class G {
        int value;
    }

    public static class RequestScopeController extends Controller {
        public Renderer check() {
            Assert.assertEquals(0, DependencyFactory.get(G.class).value++);
            Assert.assertEquals(1, DependencyFactory.get(G.class).value);
            return renderString("ok");
        }
    }

    @Test
    public void testRequest() throws IOException {
        Assert.assertEquals("ok", makeRequest("/request_scope").get().asString());
        Assert.assertEquals("ok", makeRequest("/request_scope").get().asString());
    }

    @Injectable(scope = DependencyScope.PROTOTYPE)
    static class IntHolder implements PostConstruct {
        int value = 0;

        @Override
        public void init() {
            value = 1;
        }
    }

    @Test
    public void testPostConstruct() throws IOException {
        IntHolder holder = DependencyFactory.get(IntHolder.class);
        Assert.assertEquals(1, holder.value);
        Assert.assertNotSame(holder, DependencyFactory.get(IntHolder.class));
    }


    interface TestInterface extends Service {
    }

    public static class TestInterfaceImpl implements TestInterface {
    }

    public static class TestInterfaceHolder implements Service {
        TestInterface testInterface;
    }

    @Test
    public void test_inject_interface_implementation() {
        TestInterfaceHolder holder = DependencyFactory.get(TestInterfaceHolder.class);
        Assert.assertTrue(holder.testInterface != null);
    }


    interface TestInterface2 extends Service, PostConstruct {
    }

    public static class TestInterfaceImpl2 implements TestInterface2 {
        static AtomicInteger counter = new AtomicInteger();

        @Override
        public void init() {
            counter.incrementAndGet();
            throw new IllegalStateException("init failed");
        }
    }

    public static class TestInterfaceHolder2 implements Service {
        TestInterface2 testInterface;
    }

    public static class TestInterfaceHolderHolder2 implements Service {
        TestInterfaceHolder2 testInterfaceHolder2;
    }

    @Test
    public void test_try_to_init_only_once() throws IOException {
        try {
            TestInterfaceHolderHolder2 holder = DependencyFactory.get(TestInterfaceHolderHolder2.class);
            Assert.assertTrue(false);
        } catch (Exception ignored) {
        }
        Assert.assertEquals(1, TestInterfaceImpl2.counter.get());
    }


    interface JustInterface {
    }

    static public class JustInterfaceImpl implements JustInterface {
    }

    static class JustInterfaceHolder implements Service {
        JustInterface justInterface;
    }

    @Test
    public void test_do_not_inject_everything() throws IOException {
        JustInterfaceHolder holder = DependencyFactory.get(JustInterfaceHolder.class);
        Assert.assertNull(holder.justInterface);
    }


    @Injectable(forge = CustomFactoryForge.class, scope = DependencyScope.PROTOTYPE)
    interface CustomFactoryInterface {
    }

    public static class CustomFactoryInterfaceImpl implements CustomFactoryInterface {
        public CustomFactoryInterfaceImpl(String s) {
        }
    }

    public static class CustomFactoryForge implements DependencyForge, Service {

        @Override
        public <T> Supplier<T> createSupplier(Class<? extends T> clazz) {
            if (clazz.equals(CustomFactoryInterface.class) || clazz.equals(CustomFactoryInterfaceImpl.class))
                return () -> (T) new CustomFactoryInterfaceImpl("foo");
            else
                return null;
        }
    }

    @Test
    public void test_custom_factories() throws IOException {
        CustomFactoryInterface anInterface = DependencyFactory.get(CustomFactoryInterface.class);
        Assert.assertNotNull(anInterface);
        Assert.assertEquals(CustomFactoryInterfaceImpl.class, anInterface.getClass());

        CustomFactoryInterface anotherInstance = DependencyFactory.get(CustomFactoryInterface.class);
        Assert.assertNotNull(anotherInstance);
        Assert.assertEquals(CustomFactoryInterfaceImpl.class, anotherInstance.getClass());

        Assert.assertFalse(anInterface == anotherInstance);
    }

    @Injectable
    static class FieldInjectedByName {
        String foo;
    }

    @Test
    public void test_inject_by_name() throws IOException {
        DependencyFactory.get().register("foo", "bar");

        FieldInjectedByName fieldInjectedByName = DependencyFactory.get(FieldInjectedByName.class);
        Assert.assertEquals("bar", fieldInjectedByName.foo);
    }

    @Injectable
    public static class ConstructorInjectable {
        final A a;

        public ConstructorInjectable(A a) {
            this.a = a;
        }
    }

    @Test
    public void test_constructor_injection() {
        ConstructorInjectable b = DependencyFactory.get(ConstructorInjectable.class);

        Assert.assertNotNull(b);
        Assert.assertNotNull(b.a);
    }
}
