package com.wizzardo.http.framework.template;

import com.wizzardo.epoll.readable.ReadableData;

public class ReadableDataRenderer extends Renderer {
    private final ReadableData data;

    public ReadableDataRenderer(ReadableData data) {
        this.data = data;
    }

    @Override
    public RenderResult render() {
        throw new IllegalStateException();
    }

    @Override
    public ReadableData renderReadableData() {
        return data;
    }
}
