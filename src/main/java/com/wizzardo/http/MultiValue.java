package com.wizzardo.http;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: wizzardo
 * Date: 7/26/14
 */
public class MultiValue {
    private String value;
    private List<String> values;

    public MultiValue() {
    }

    public MultiValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public List<String> getValues() {
        if (values == null) {
            values = new ArrayList<>();
            values.add(value);
        }
        return values;
    }

    public void append(String value) {
        if (this.value == null)
            this.value = value;
        else
            getValues().add(value);
    }

    public String[] asArray() {
        return getValues().toArray(new String[getValues().size()]);
    }

    @Override
    public String toString() {
        if (values == null)
            return value;
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
