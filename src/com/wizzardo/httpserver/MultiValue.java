package com.wizzardo.httpserver;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: wizzardo
 * Date: 7/26/14
 */
public class MultiValue {
    private String value;
    private List<String> values;

    public MultiValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public List<String> getValues() {
        if (values == null) {
            values = new ArrayList<String>();
            values.add(value);
        }
        return values;
    }

    public void append(String value) {
        getValues().add(value);
    }
}
