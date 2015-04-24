package com.wizzardo.http.template;

import com.wizzardo.epoll.readable.ReadableData;

import java.util.Map;

/**
 * @author: moxa
 * Date: 2/11/13
 */
public interface Renderable {
    RenderResult get(Map<String, Object> model);
}
