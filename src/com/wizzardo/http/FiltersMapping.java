/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wizzardo.http;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Moxa
 */
public class FiltersMapping extends Filter {

    protected Map<String, List<Filter>> mapping = new HashMap<String, List<Filter>>();
    protected Map<Pattern, List<Filter>> regexpMapping = new LinkedHashMap<Pattern, List<Filter>>();

    public FiltersMapping add(String url, final Filter handler) {
        if (url.contains("*"))
            add(Pattern.compile(url.replace("*", ".*")), handler);
        else {
            List<Filter> l = mapping.get(url);
            if (l == null) {
                l = new ArrayList<>();
                mapping.put(url, l);
            }
            l.add(handler);
        }
        return this;
    }

    public FiltersMapping add(Pattern url, Filter handler) {
        List<Filter> l = regexpMapping.get(url);
        if (l == null) {
            l = new ArrayList<>();
            regexpMapping.put(url, l);
        }
        l.add(handler);
        return this;
    }


    @Override
    public boolean before(Request request, Response response) {
        List<Filter> filters = mapping.get(request.path());
        if (filters != null)
            if (!before(mapping.get(request.path()), request, response))
                return false;

        for (Map.Entry<Pattern, List<Filter>> entry : regexpMapping.entrySet()) {
            if (entry.getKey().matcher(request.path()).matches()) {
                if (!before(regexpMapping.get(entry.getKey()), request, response))
                    return false;
            }
        }

        return true;
    }

    @Override
    public boolean after(Request request, Response response) {
        List<Filter> filters = mapping.get(request.path());
        if (filters != null)
            if (!after(mapping.get(request.path()), request, response))
                return false;

        for (Map.Entry<Pattern, List<Filter>> entry : regexpMapping.entrySet()) {
            if (entry.getKey().matcher(request.path()).matches()) {
                if (!after(regexpMapping.get(entry.getKey()), request, response))
                    return false;
            }
        }

        return true;
    }

    private boolean before(List<Filter> filters, Request request, Response response) {
        for (Filter f : filters) {
            if (!f.before(request, response))
                return false;

        }
        return true;
    }


    private boolean after(List<Filter> filters, Request request, Response response) {
        for (Filter f : filters) {
            if (!f.after(request, response))
                return false;
        }
        return true;
    }
}