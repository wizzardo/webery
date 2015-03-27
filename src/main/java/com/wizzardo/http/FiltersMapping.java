/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wizzardo.http;

import com.wizzardo.http.mapping.ChainUrlMapping;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

/**
 * @author Moxa
 */
public class FiltersMapping {

    protected ChainUrlMapping<Filter> before = new ChainUrlMapping<>();
    protected ChainUrlMapping<Filter> after = new ChainUrlMapping<>();

    public FiltersMapping addBefore(String url, Filter handler) {
        before.add(url, handler);
        return this;
    }

    public FiltersMapping addAfter(String url, Filter handler) {
        after.add(url, handler);
        return this;
    }

    public boolean filter(Request request, Response response, ChainUrlMapping<Filter> mapping) {
        ChainUrlMapping.Chain<Filter> filters = mapping.get(request);
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

    protected boolean filter(ChainUrlMapping.Chain<Filter> filters, Request request, Response response) {
        for (Filter f : filters) {
            if (!f.filter(request, response))
                return false;

        }
        return true;
    }
}