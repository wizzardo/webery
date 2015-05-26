/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wizzardo.http.framework.template;

import java.util.HashMap;

/**
 * @author Moxa
 */
public class Model extends HashMap<String, Object> {

    public Model append(String key, Object value) {
        put(key, value);
        return this;
    }
}
