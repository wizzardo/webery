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
public class FiltersMapping {

    protected Map<String, List<Filter>> mappingBefore = new HashMap<String, List<Filter>>();
    protected Map<Pattern, List<Filter>> regexpMappingBefore = new LinkedHashMap<Pattern, List<Filter>>();

    protected Map<String, List<Filter>> mappingAfter = new HashMap<String, List<Filter>>();
    protected Map<Pattern, List<Filter>> regexpMappingAfter = new LinkedHashMap<Pattern, List<Filter>>();

    public FiltersMapping addBefore(String url, Filter handler) {
        return add(url, handler, mappingBefore, regexpMappingBefore);
    }

    public FiltersMapping addAfter(String url, Filter handler) {
        return add(url, handler, mappingAfter, regexpMappingAfter);
    }

    protected FiltersMapping add(String url, Filter handler, Map<String, List<Filter>> mapping, Map<Pattern, List<Filter>> regexpMapping) {
        if (url.contains("*"))
            add(Pattern.compile(url.replace("*", ".*")), handler, regexpMapping);
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

    protected FiltersMapping add(Pattern url, Filter handler, Map<Pattern, List<Filter>> regexpMapping) {
        List<Filter> l = regexpMapping.get(url);
        if (l == null) {
            l = new ArrayList<>();
            regexpMapping.put(url, l);
        }
        l.add(handler);
        return this;
    }

    public boolean filter(Request request, Response response, Map<String, List<Filter>> mapping, Map<Pattern, List<Filter>> regexpMapping) {
        List<Filter> filters = mapping.get(request.path().toString());
        if (filters != null)
            if (!filter(mapping.get(request.path().toString()), request, response))
                return false;

        for (Map.Entry<Pattern, List<Filter>> entry : regexpMapping.entrySet()) {
            if (entry.getKey().matcher(request.path().toString()).matches()) {
                if (!filter(regexpMapping.get(entry.getKey()), request, response))
                    return false;
            }
        }

        return true;
    }

    public boolean before(Request request, Response response) {
        return filter(request, response, mappingBefore, regexpMappingBefore);
    }

    public boolean after(Request request, Response response) {
        return filter(request, response, mappingAfter, regexpMappingAfter);
    }

    protected boolean filter(List<Filter> filters, Request request, Response response) {
        for (Filter f : filters) {
            if (!f.filter(request, response))
                return false;

        }
        return true;
    }
}