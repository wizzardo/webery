package com.wizzardo.http.framework.template;

/**
 * @author: moxa
 * Date: 5/6/13
 */
public class TextRenderer extends Renderer {
    private String text;

    public TextRenderer(String text) {
        this.text = text;
    }

    @Override
    public RenderResult render() {
        return new RenderResult(text);
    }
}
