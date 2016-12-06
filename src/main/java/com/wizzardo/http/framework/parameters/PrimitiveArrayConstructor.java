package com.wizzardo.http.framework.parameters;

import com.wizzardo.http.MultiValue;
import com.wizzardo.http.request.Request;
import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.interfaces.Mapper;

import java.util.Arrays;
import java.util.List;

/**
 * Created by wizzardo on 25/10/16.
 */
public class PrimitiveArrayConstructor<T> implements Mapper<Request, Object> {
    final String name;
    final Mapper<Integer, T> creator;
    final Mapper<T, T> cloner;
    final CollectionTools.Closure2<T, T, List<String>> populator;
    final T def;

    PrimitiveArrayConstructor(String name, String def, Mapper<Integer, T> creator, CollectionTools.Closure2<T, T, List<String>> populator, Mapper<T, T> cloner) {
        this.name = name;
        this.creator = creator;
        this.populator = populator;
        this.cloner = cloner;
        if (def != null && !def.isEmpty()) {
            List<String> strings = Arrays.asList(def.split(","));
            this.def = populator.execute(creator.map(strings.size()), strings);
        } else {
            this.def = null;
        }
    }

    @Override
    public T map(Request request) {
        MultiValue multiValue = request.params().get(name);
        if (multiValue != null) {
            T t = creator.map(multiValue.size());
            return populator.execute(t, multiValue.getValues());
        }

        if (def == null)
            return null;

        return cloner.map(def);
    }
}
