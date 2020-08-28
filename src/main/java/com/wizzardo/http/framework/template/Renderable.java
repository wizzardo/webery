package com.wizzardo.http.framework.template;

import java.util.Map;

/**
 * @author: moxa
 * Date: 2/11/13
 */
public interface Renderable {
    default RenderResult get(Map<String, Object> model) {
       return get(model, new RenderResult());
    }

    RenderResult get(Map<String, Object> model, RenderResult into);

    static Renderable create(String s) {
        RenderResult result = new RenderResult(s);
        return (model, into) -> into.append(result);
    }
}
