package com.wizzardo.http.mapping;

/**
* Created by wizzardo on 27.03.15.
*/
class UrlMappingHolder<T> extends UrlMapping<T> {
    protected UrlMappingHolder(UrlMapping<T> parent) {
        super(parent);
    }

    @Override
    protected boolean checkNextPart() {
        return false;
    }
}
