package com.wizzardo.http.template;

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
    protected RenderResult render() {
        return new RenderResult(text);
    }
}
