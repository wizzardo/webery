package com.wizzardo.http.framework.template;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

/**
 * @author: moxa
 * Date: 5/24/13
 */
public class RenderableList extends ArrayList<Renderable> implements Renderable {

    protected BytesHolder lastStatic;

    @Override
    public RenderResult get(Map<String, Object> model) {
        RenderResult result = new RenderResult();
        for (Renderable renderable : this) {
            result.append(renderable.get(model));
        }
        return result;
    }

    public RenderableList append(String s) {
        return append(s.getBytes(StandardCharsets.UTF_8));
    }

    public RenderableList append(byte[] bytes) {
        if (lastStatic != null)
            lastStatic.append(bytes);
        else {
            BytesHolder bytesHolder = new BytesHolder(bytes);
            add(bytesHolder);
            lastStatic = bytesHolder;
        }
        return this;
    }

    @Override
    public boolean add(Renderable renderable) {
        if (lastStatic != null && lastStatic == renderable)
            return true;
        else
            lastStatic = null;

        return super.add(renderable);
    }

    public RenderableList append(RenderableList renderables) {
        for (Renderable renderable : renderables) {
            add(renderable);
        }
        return this;
    }
}
