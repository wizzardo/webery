package com.wizzardo.http;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: wizzardo
 * Date: 7/26/14
 */
public class MultiValue<T> {
    private T value;
    private List<T> values;

    public MultiValue() {
    }

    public MultiValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public T value() {
        return value;
    }

    public List<T> getValues() {
        if (values == null) {
            values = new ArrayList<>();
            values.add(value);
        }
        return values;
    }

    public void append(T value) {
        if (this.value == null)
            this.value = value;
        else
            getValues().add(value);
    }

    public T[] asArray() {
        return (T[]) getValues().toArray();
    }

    @Override
    public String toString() {
        if (values == null)
            return String.valueOf(value);
        else
            return values.toString();
    }

    public int size() {
        if (values == null)
            return value == null ? 0 : 1;
        else
            return values.size();
    }
}
