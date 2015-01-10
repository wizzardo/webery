package com.wizzardo.http;

import com.wizzardo.tools.http.Cookie;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * @author: wizzardo
 * Date: 29.09.14
 */
public class SessionTest extends ServerTest {

    @Test
    public void test() throws IOException {
        handler = new UrlMapping()
                .append("/set", (request, response) -> {
                    request.session().put("key", "value");
                    return response.setBody("ok");
                }).append("/get", (request, response) -> response.setBody(request.session().get("key").toString()));

        List<Cookie> cookies = makeRequest("/set").get().getCookies();
//        System.out.println(cookies);

        Assert.assertEquals("value", makeRequest("/get").cookies(cookies).get().asString());
    }
}
