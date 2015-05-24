package com.wizzardo.http.framework.template;

import java.util.Map;

/**
 * @author: moxa
 * Date: 2/11/13
 */
public interface Renderable {
    RenderResult get(Map<String, Object> model);

    static Renderable create(String s) {
        RenderResult result = new RenderResult(s);
        return model -> result;
    }
}
