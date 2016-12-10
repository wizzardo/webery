/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wizzardo.http.request;

import com.wizzardo.http.MultiValue;
import com.wizzardo.tools.interfaces.Mapper;
import com.wizzardo.tools.misc.TextTools;

import java.util.LinkedHashMap;

public class Parameters extends LinkedHashMap<String, MultiValue> {

    protected String getSingleValue(String key) {
        MultiValue multiValue = get(key);
        return multiValue == null ? null : multiValue.getValue();
    }

    public String get(String key, String def) {
        String value = getSingleValue(key);
        return value == null ? def : value;
    }

    public <T> T map(String key, Mapper<String, T> mapper) {
        return map(key, null, mapper);
    }

    public <T> T map(String key, T def, Mapper<String, T> mapper) {
        String value = getSingleValue(key);
        if (value == null)
            return def;

        return mapper.map(value);
    }

    public String string(String key) {
        return getSingleValue(key);
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
