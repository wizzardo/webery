package com.wizzardo.http.framework.parameters;

import com.wizzardo.http.MultiValue;
import com.wizzardo.http.request.Parameters;
import com.wizzardo.http.request.Request;
import com.wizzardo.tools.misc.Mapper;

import java.util.Arrays;
import java.util.List;

/**
 * Created by wizzardo on 25/10/16.
 */
public class ArrayConstructor<T> implements Mapper<Request, Object> {
    final String name;
    final Mapper<Integer, T[]> creator;
    final Mapper<String, T> converter;
    final T[] def;

    ArrayConstructor(String name, String def, Mapper<Integer, T[]> creator, Mapper<String, T> converter) {
        this.name = name;
        this.creator = creator;
        this.converter = converter;
        if (def != null && !def.isEmpty()) {
            List<String> strings = Arrays.asList(def.split(","));
            this.def = creator.map(strings.size());
            populate(this.def, strings, converter);
        } else {
            this.def = null;
        }
    }

    @Override
    public T[] map(Request request) {
        MultiValue multiValue = request.params().get(name);
        if (multiValue != null) {
            T[] arr = creator.map(multiValue.size());
            populate(arr, multiValue.getValues(), converter);
            return arr;
        }

        if (def == null)
            return null;

        return Arrays.copyOf(def, def.length);
    }

    protected void populate(T[] arr, List<String> values, Mapper<String, T> converter) {
        for (int i = 0; i < values.size(); i++) {
            arr[i] = converter.map(values.get(i));
        }
    }
}
