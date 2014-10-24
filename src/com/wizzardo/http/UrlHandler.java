package com.wizzardo.http;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: wizzardo
 * Date: 25.09.14
 */
public class UrlHandler implements Handler {

    private static Pattern VARIABLES = Pattern.compile("\\$\\{?([a-zA-Z_]+[\\w]*)\\}?");

    protected HashMap<String, Handler> mapping = new HashMap<>();
    protected LinkedHashMap<Pattern, Handler> regexpMapping = new LinkedHashMap<>();

    @Override
    public Response handle(Request request, Response response) throws IOException {
        Handler handler = mapping.get(request.path());
        if (handler != null)
            return handler.handle(request, response);

        for (Map.Entry<Pattern, Handler> entry : regexpMapping.entrySet()) {
            if (entry.getKey().matcher(request.path()).matches())
                return entry.getValue().handle(request, response);
        }


        return response.setStatus(Status._404).setBody(request.path() + " not found");
    }

    public UrlHandler append(String url, Handler handler) {
        if (url.contains("*")) {
            regexpMapping.put(Pattern.compile(url.replace("*", ".*")), handler);
        } else if (url.contains("$")) {
            Matcher m = VARIABLES.matcher(url);
            final List<String> vars = new ArrayList<String>();
            while (m.find()) {
                vars.add(m.group(1));
            }
            url = url.replaceAll(VARIABLES.pattern(), "([^/]+)").replace("/([^/]+)?", "/?([^/]+)?");

            Pattern pattern = Pattern.compile(url);
            String[] variables = vars.toArray(new String[vars.size()]);

            regexpMapping.put(pattern, (request, response) -> {
                Matcher matcher = pattern.matcher(request.path());
                if (matcher.find()) {
                    for (int i = 1; i <= variables.length; i++) {
                        request.param(variables[i - 1], matcher.group(i));
                    }
                }
                return handler.handle(request, response);
            });
        } else {
            mapping.put(url, handler);
        }

        return this;
    }
}
