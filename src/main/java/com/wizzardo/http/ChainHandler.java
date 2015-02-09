package com.wizzardo.http;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author: wizzardo
 * Date: 07.09.14
 */
public class ChainHandler implements Handler {
    protected List<Link> handlers = new ArrayList<>();

    @Override
    public Response handle(Request request, Response response) {
        Iterator<Link> iterator = handlers.iterator();
        while (!response.isCommitted() && iterator.hasNext()) {
            Link link = iterator.next();
            if (!link.handle(request, response))
                break;
        }

        return response;
    }

    public ChainHandler append(Link link) {
        handlers.add(link);
        return this;
    }

    public static interface Link {
        public boolean handle(Request request, Response response);
    }

}
