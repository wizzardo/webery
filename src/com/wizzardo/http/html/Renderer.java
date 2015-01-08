package com.wizzardo.http.html;

/**
 * Created by wizzardo on 08.01.15.
 */
public interface Renderer {
    Renderer append(String s);

    Renderer append(char ch);

    public static Renderer create(StringBuilder sb) {
        return new Renderer() {
            @Override
            public Renderer append(String s) {
                sb.append(s);
                return this;
            }

            @Override
            public Renderer append(char ch) {
                sb.append(ch);
                return this;
            }
        };
    }
}
