package com.wizzardo.http.framework.parameters;

import com.wizzardo.http.MultiValue;
import com.wizzardo.http.request.Parameters;
import com.wizzardo.tools.misc.Mapper;
import com.wizzardo.tools.misc.Supplier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by wizzardo on 25/10/16.
 */
public class CollectionConstructor<C extends Collection<T>, T> implements Mapper<Parameters, Object> {
    final String name;
    final Supplier<C> supplier;
    final Mapper<String, T> converter;
    final List<T> def;

    public CollectionConstructor(String name, String def, Supplier<C> supplier, Mapper<String, T> converter) {
        this.name = name;
        this.supplier = supplier;
        this.converter = converter;
        if (def != null && !def.isEmpty()) {
            populate(this.def = new ArrayList<>(), Arrays.asList(def.split(",")), converter);
        } else {
            this.def = null;
        }
    }

    @Override
    public C map(Parameters parameters) {
        MultiValue multiValue = parameters.get(name);
        if (multiValue != null) {
            C arr = supplier.supply();
            populate(arr, multiValue.getValues(), converter);
            return arr;
        }

        if (def == null)
            return null;

        C c = supplier.supply();
        c.addAll(def);
        return c;
    }

    protected void populate(Collection<T> c, List<String> values, Mapper<String, T> converter) {
        for (String value : values) {
            c.add(converter.map(value));
        }
    }
}
