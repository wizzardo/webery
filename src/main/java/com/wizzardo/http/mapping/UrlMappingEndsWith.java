package com.wizzardo.http.mapping;

import com.wizzardo.http.Named;
import com.wizzardo.tools.misc.CharTree;

import java.util.List;

/**
* Created by wizzardo on 27.03.15.
*/
class UrlMappingEndsWith<T extends Named> extends UrlMapping<T> {
    protected CharTree<UrlMapping<T>> endsWith = new CharTree<>();

    protected UrlMappingEndsWith(UrlMapping<T> parent) {
        super(parent);
    }

    UrlMapping<T> append(String pattern) {
        UrlMappingHolder<T> mapping = new UrlMappingHolder<>(this);
        endsWith.appendReverse(pattern, mapping);
        return mapping;
    }

    @Override
    protected UrlMapping<T> find(String part, List<String> parts) {
        return endsWith.findEnds(parts.get(parts.size() - 1));
    }

    @Override
    protected UrlMapping<T> find(String part, String[] parts) {
        return endsWith.findEnds(parts[parts.length - 1]);
    }
}
