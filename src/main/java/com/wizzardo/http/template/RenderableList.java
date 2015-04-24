package com.wizzardo.http.template;

import com.wizzardo.epoll.readable.ReadableBuilder;
import com.wizzardo.epoll.readable.ReadableData;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author: moxa
 * Date: 5/24/13
 */
public class RenderableList extends ArrayList<Renderable> implements Renderable {

    @Override
    public ReadableData get(Map<String, Object> model) {
        ReadableBuilder result = new ReadableBuilder();
        for (Renderable renderable : this) {
            result.append(renderable.get(model));
        }
        return result;
    }

    public RenderableList append(String s) {
        add(new BytesHolder(s));
        return this;
    }

    public RenderableList append(byte[] bytes) {
        add(new BytesHolder(bytes));
        return this;
    }

    public RenderableList append(RenderableList renderables) {
        addAll(renderables);
        return this;
    }
}
