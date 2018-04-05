package com.wizzardo.http;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by wizzardo on 07.06.15.
 */
public class AsyncTest extends ServerTest {

    @Test
    public void test_1() throws IOException {
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
        ThreadGroup group = new ThreadGroup("async_test_group");
        new Worker<Runnable>(group, queue) {
            @Override
            protected void process(Runnable runnable) {
                runnable.run();
            }
        }.start();


        handler = new UrlHandler()
                .append("/async", (request, response) -> {
                    response.async();
                    queue.add(() -> {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                        response.setBody("ok");
                        response.commit(request.connection());
                        request.connection().flush();
                        request.connection().onFinishingHandling();
                    });
                    return response;
                })
        ;


        Assert.assertEquals("ok", makeRequest("/async").get().asString());
        Assert.assertEquals("ok", makeRequest("/async").get().asString());
        Assert.assertEquals("ok", makeRequest("/async").get().asString());
    }
}
