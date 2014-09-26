package com.wizzardo.http.request;

import com.wizzardo.http.Filter;
import com.wizzardo.http.UrlHandler;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author: wizzardo
 * Date: 26.09.14
 */
public class FiltersTest extends ServerTest {
    @Override
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        super.setUp();
        handler = new UrlHandler()
                .append("/allowed", request -> new Response().setBody("ok"))
                .append("/notAllowed", request -> new Response().setBody("ok"))
                .append("/say/$what", request -> new Response().setBody("I say: " + request.param("what")))
        ;
        server.getFiltersMapping().add("/notAllowed", new Filter() {

            @Override
            public boolean before(Request request, Response response) {
                boolean allowed = "true".equals(request.param("butAllowed"));
                if (!allowed) {
                    response.setStatus(Status._403);
                }
                return allowed;
            }
        });

        server.getFiltersMapping().add("/say/*", new Filter() {
            @Override
            public boolean after(Request request, Response response) {
                if (!"true".equals(request.param("gzip")))
                    return true;

                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    GZIPOutputStream gout = new GZIPOutputStream(out);
                    gout.write(response.getBody());
                    gout.close();
                    response.setBody(out.toByteArray());
                    response.setHeader("Content-Encoding", "gzipped"); // should be 'gzip', but for test it's 'gzipped' because HttpClient unzips data
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                return true;
            }
        });
    }

    @Test
    public void allowed() throws IOException {
        Assert.assertEquals("ok", makeRequest("/allowed").get().asString());
    }

    @Test
    public void notAllowed() throws IOException {
        Assert.assertEquals(403, makeRequest("/notAllowed").get().getResponseCode());
        Assert.assertEquals("", makeRequest("/notAllowed").get().asString());

        Assert.assertEquals(200, makeRequest("/notAllowed?butAllowed=true").get().getResponseCode());
        Assert.assertEquals("ok", makeRequest("/notAllowed?butAllowed=true").get().asString());
    }


    @Test
    public void after() throws IOException {
        Assert.assertEquals("I say: helloooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo", makeRequest("/say/helloooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo").get().asString());

        byte[] bytes = makeRequest("/say/helloooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo?gzip=true").get().asBytes();
        Assert.assertNotEquals("I say: helloooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo", new String(bytes));

        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        GZIPInputStream gin = new GZIPInputStream(in, bytes.length);
        bytes = new byte[bytes.length];

        int r;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((r = gin.read(bytes)) != -1) {
            out.write(bytes, 0, r);
        }
        bytes = out.toByteArray();

        Assert.assertEquals("I say: helloooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo", new String(bytes));
    }
}
