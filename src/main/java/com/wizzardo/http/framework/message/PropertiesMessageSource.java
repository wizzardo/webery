package com.wizzardo.http.framework.message;

import com.wizzardo.http.framework.template.ResourceTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * Created by wizzardo on 22.05.15.
 */
public class PropertiesMessageSource extends TemplateMessageSource {

    public PropertiesMessageSource(String name, ResourceTools resourcesTools) {
        Properties properties = new Properties();
        if (!name.endsWith(".properties"))
            name += ".properties";

        String path = name;
        try {
            Unchecked.run(() -> properties.load(new InputStreamReader(resourcesTools.getResource("/i18n/" + path), StandardCharsets.UTF_8)));
        } catch (Exception e) {
            Unchecked.run(() -> properties.load(new InputStreamReader(resourcesTools.getResource(path), StandardCharsets.UTF_8)));
        }


        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            templates.put(String.valueOf(entry.getKey()), new Template(String.valueOf(entry.getValue())));
        }
    }
}
