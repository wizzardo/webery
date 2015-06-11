package com.wizzardo.http.framework.template;

import java.util.Map;

/**
 * Created by wizzardo on 10.06.15.
 */
public interface RenderableString {

    String render(Map<String, Object> model);

}
