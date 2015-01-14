/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wizzardo.http;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Moxa
 */
public class FiltersMapping {

    protected UrlMapping<List<Filter>> before = new UrlMapping<>();
    protected UrlMapping<List<Filter>> after = new UrlMapping<>();

    public FiltersMapping addBefore(String url, Filter handler) {
        return add(url, handler, before);
    }

    public FiltersMapping addAfter(String url, Filter handler) {
        return add(url, handler, after);
    }

    protected FiltersMapping add(String url, Filter handler, UrlMapping<List<Filter>> mapping) {
        List<Filter> list = mapping.get(url);
        if (list == null)
            mapping.append(url, list = new ArrayList<>());

        list.add(handler);
        return this;
    }

    public boolean filter(Request request, Response response, UrlMapping<List<Filter>> mapping) {
        List<Filter> filters = mapping.get(request);
        if (filters != null)
            if (!filter(filters, request, response))
                return false;

        return true;
    }

    public boolean before(Request request, Response response) {
        return filter(request, response, before);
    }

    public boolean after(Request request, Response response) {
        return filter(request, response, after);
    }

    protected boolean filter(List<Filter> filters, Request request, Response response) {
        for (Filter f : filters) {
            if (!f.filter(request, response))
                return false;

        }
        return true;
    }
}