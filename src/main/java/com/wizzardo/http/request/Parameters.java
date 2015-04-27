/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wizzardo.http.request;

import com.wizzardo.http.MultiValue;
import com.wizzardo.tools.misc.TextTools;

import java.util.LinkedHashMap;

/**
 * @author Moxa
 */
public class Parameters extends LinkedHashMap<String, MultiValue> {

    protected String getSingleValue(String key) {
        MultiValue multiValue = get(key);
        return multiValue == null ? null : multiValue.getValue();
    }

    public String get(String key, String def) {
        String value = getSingleValue(key);
        return value == null ? def : value;
    }

    public int getInt(String key) {
        return TextTools.asInt(getSingleValue(key));
    }

    public int getInt(String key, int def) {
        return TextTools.asInt(getSingleValue(key), def);
    }

    public long getLong(String key) {
        return TextTools.asLong(getSingleValue(key));
    }

    public long getLong(String key, long def) {
        return TextTools.asLong(getSingleValue(key), def);
    }
}
