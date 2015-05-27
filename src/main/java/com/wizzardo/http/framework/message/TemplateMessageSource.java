package com.wizzardo.http.framework.message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wizzardo on 23.05.15.
 */
public class TemplateMessageSource implements MessageSource {
    protected Map<String, Template> templates = new HashMap<>();

    @Override
    public String get(String key, Args args) {
        Template template = templates.get(key);
        return template == null ? null : template.get(args);
    }
}
