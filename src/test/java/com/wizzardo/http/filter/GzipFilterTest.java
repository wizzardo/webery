package com.wizzardo.http.filter;

import com.wizzardo.http.ServerTest;
import com.wizzardo.http.request.Header;
import com.wizzardo.tools.http.Response;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class GzipFilterTest extends ServerTest {

    @Test
    public void test_gzip() throws IOException {
        String data = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis feugiat nibh eu dapibus interdum. Fusce tincidunt neque nec urna rutrum, eget tristique nulla cursus. Aliquam eu convallis libero posuere.";
        handler = (request, response) -> response.setBody(data);
        server.getFiltersMapping().addAfter("/*", new GzipFilter());

        Response response = makeRequest("/").get();
        Assert.assertEquals(data, response.asString());
        Assert.assertEquals("gzip", response.header("Content-Encoding"));
        Assert.assertEquals(158, response.getContentLength());
    }

    @Test
    public void test_gzip_double_mapping() throws IOException {
        server.getFiltersMapping().addAfter("/*", new GzipFilter());
        test_gzip();
    }

    @Test
    public void test_no_gzip_for_empty_response() throws IOException {
        String data = "";
        handler = (request, response) -> response.setBody(data);
        server.getFiltersMapping().addAfter("/*", new GzipFilter());

        Response response = makeRequest("/").get();
        Assert.assertEquals(data, response.asString());
        Assert.assertEquals(null, response.header("Content-Encoding"));
        Assert.assertEquals(0, response.getContentLength());


        handler = (request, resp) -> resp.setBody(data).header(Header.KEY_CONTENT_ENCODING, Header.VALUE_GZIP);

        response = makeRequest("/").get();
        Assert.assertEquals("gzip", response.header("Content-Encoding"));
        Assert.assertEquals(0, response.getContentLength());
    }
}