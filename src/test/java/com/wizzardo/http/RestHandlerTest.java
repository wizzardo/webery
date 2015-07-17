package com.wizzardo.http;

import com.wizzardo.tools.http.ConnectionMethod;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by wizzardo on 16.04.15.
 */
public class RestHandlerTest extends ServerTest {

    @Test
    public void test_allowPostAndPut() throws IOException {
        handler = new UrlHandler()
                .append("/rest", new RestHandler()
                        .post(
                                (request, response) -> response.setBody("post")
                        )
                        .put(
                                (request, response) -> response.setBody("put")
                        ))
        ;

        Assert.assertEquals("post", makeRequest("/rest").post().asString());
        Assert.assertEquals("put", makeRequest("/rest").method(ConnectionMethod.PUT).execute().asString());
        Assert.assertEquals(405, makeRequest("/rest").get().getResponseCode());
        Assert.assertEquals("POST, PUT", makeRequest("/rest").get().header("Allow"));
    }

    @Test
    public void test_allowGetAndDelete() throws IOException {
        handler = new UrlHandler()
                .append("/rest", new RestHandler()
                        .get(
                                (request, response) -> response.setBody("get")
                        )
                        .delete(
                                (request, response) -> response.setBody("delete")
                        ))
        ;

        Assert.assertEquals("get", makeRequest("/rest").get().asString());
        Assert.assertEquals("delete", makeRequest("/rest").method(ConnectionMethod.DELETE).execute().asString());
        Assert.assertEquals(405, makeRequest("/rest").post().getResponseCode());
        Assert.assertEquals("GET, DELETE", makeRequest("/rest").post().header("Allow"));
    }

    @Test
    public void test_allowNothing() throws IOException {
        handler = new UrlHandler()
                .append("/rest", new RestHandler().get(null))
        ;

        Assert.assertEquals(405, makeRequest("/rest").get().getResponseCode());
        Assert.assertEquals(null, makeRequest("/rest").get().header("Allow"));

        Assert.assertEquals(405, makeRequest("/rest").method(ConnectionMethod.HEAD).execute().getResponseCode());
    }

}
