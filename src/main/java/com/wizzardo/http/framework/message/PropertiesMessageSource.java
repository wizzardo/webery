package com.wizzardo.http.framework.message;

import com.wizzardo.tools.misc.Unchecked;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by wizzardo on 22.05.15.
 */
public class PropertiesMessageSource extends TemplateMessageSource {

    public PropertiesMessageSource(String name) {
        Properties properties = new Properties();
        Unchecked.run(() -> properties.load(PropertiesMessageSource.class.getResourceAsStream("/i18n/" + name + ".properties")));

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            templates.put(String.valueOf(entry.getKey()), new Template(String.valueOf(entry.getValue())));
        }
    }
}
