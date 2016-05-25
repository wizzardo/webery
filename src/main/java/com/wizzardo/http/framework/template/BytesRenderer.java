package com.wizzardo.http.framework.template;

public class BytesRenderer extends Renderer {
    private byte[] bytes;

    public BytesRenderer(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public RenderResult render() {
        return new RenderResult(bytes);
    }
}
