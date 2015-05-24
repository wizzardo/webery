package com.wizzardo.http.framework.message;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wizzardo on 22.05.15.
 */
public class MessageBundle implements MessageSource {

    protected List<String> sourcesNames = new ArrayList<>();
    protected TemplateMessageSource defaultSource = new TemplateMessageSource();
    protected Map<Locale, TemplateMessageSource> sources = new ConcurrentHashMap<>();
    protected TemplateMessageSource defaultLocalizedSource;
    protected Locale defaultLocale;

    @Override
    public String get(String key, Object... args) {
        return get(defaultLocalizedSource, key, args);
    }

    public String get(Locale locale, String key, Object... args) {
        TemplateMessageSource messageSource;
        if (locale.equals(defaultLocale))
            messageSource = defaultLocalizedSource;
        else
            messageSource = getMessageSource(locale);

        return get(messageSource, key, args);
    }

    protected String get(MessageSource messageSource, String key, Object... args) {
        if (messageSource != null) {
            String result = messageSource.get(key, args);
            if (result != null)
                return result;
        }

        return defaultSource.get(key, args);
    }

    public MessageBundle load(String name) {
        sourcesNames.add(name);
        sources.clear();

        load(name, defaultSource);

        if (defaultLocale != null)
            setDefaultLocale(defaultLocale);

        return this;
    }

    protected TemplateMessageSource getMessageSource(Locale locale) {
        TemplateMessageSource messageSource = sources.get(locale);
        if (messageSource == null) {
            messageSource = new TemplateMessageSource();
            sources.put(locale, messageSource);
            for (String sourcesName : sourcesNames) {
                load(sourcesName + "_" + locale.getLanguage(), messageSource);
            }
        }
        return messageSource;
    }

    protected void load(String name, TemplateMessageSource messageSource) {
        try {
            PropertiesMessageSource temp = new PropertiesMessageSource(name);
            messageSource.templates.putAll(temp.templates);
        } catch (Exception ignored) {
        }
    }

    public MessageBundle setDefaultLocale(Locale locale) {
        defaultLocale = locale;
        defaultLocalizedSource = getMessageSource(defaultLocale);
        return this;
    }

    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    public MessageBundle appendDefault(String key, String template) {
        defaultSource.templates.put(key, new Template(template));
        return this;
    }
}
