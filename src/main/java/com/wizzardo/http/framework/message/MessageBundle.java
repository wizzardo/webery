package com.wizzardo.http.framework.message;

import com.wizzardo.http.framework.di.DependencyFactory;
import com.wizzardo.http.framework.template.ResourceTools;

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
    protected ResourceTools resourcesTools;

    public MessageBundle() {
        this(DependencyFactory.get(ResourceTools.class));
    }

    public MessageBundle(ResourceTools resourcesTools) {
        this.resourcesTools = resourcesTools;
    }

    @Override
    public String get(String key, Args args) {
        return get(defaultLocalizedSource, key, args);
    }

    public String get(Locale locale, String key, Object... args) {
        return get(locale, key, Args.create(args));
    }

    public String get(Locale locale, String key, List args) {
        return get(locale, key, Args.create(args));
    }

    public String get(Locale locale, String key, Args args) {
        TemplateMessageSource messageSource;
        if (locale.equals(defaultLocale))
            messageSource = defaultLocalizedSource;
        else
            messageSource = getMessageSource(locale);

        return get(messageSource, key, args);
    }

    protected String get(MessageSource messageSource, String key, Args args) {
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

        load(name, defaultSource, resourcesTools);

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
                load(sourcesName + "_" + locale.getLanguage(), messageSource, resourcesTools);
            }
        }
        return messageSource;
    }

    protected void load(String name, TemplateMessageSource messageSource, ResourceTools resourcesTools) {
        try {
            PropertiesMessageSource temp = new PropertiesMessageSource(name, resourcesTools);
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
