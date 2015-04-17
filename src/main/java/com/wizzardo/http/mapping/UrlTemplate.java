package com.wizzardo.http.mapping;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wizzardo on 16.04.15.
 */
public class UrlTemplate {
    private String host;
    private List<TemplateHolder> holders = new ArrayList<TemplateHolder>();
    private static Pattern variables = Pattern.compile("(/)?\\$\\{?([a-zA-Z_]+[\\w]*)\\}?\\??");

    public UrlTemplate(String host, String url) {
        this.host = host;

        Matcher m = variables.matcher(url);
        if (!m.find()) {
            holders.add(new StringHolder(url));
        } else {
            int last = 0;
            do {
                if (last != m.start())
                    holders.add(new StringHolder(url.substring(last, m.start())));

                String prefix = null;
                if (m.start() != 0) {
                    prefix = m.group(1);
                } else {
                    holders.add(new StringHolder("/"));
                }

                holders.add(new VariableHolder(m.group(2), prefix, m.group().endsWith("?")));
                last = m.end();
            } while (m.find());
            if (last < url.length())
                holders.add(new StringHolder(url.substring(last)));
        }
    }

    public String getUrl(String base, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(base);
        return getRelativeUrl(params, sb);
    }

    public String getAbsoluteUrl(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(host);
        return getRelativeUrl(params, sb);
    }

    public String getRelativeUrl(Map params) {
        StringBuilder sb = new StringBuilder();
        return getRelativeUrl(params, sb);
    }

    private String getRelativeUrl(Map<?, ?> paramsSrc, StringBuilder sb) {
        Map<Object, Object> params;

        if (paramsSrc != null)
            params = new LinkedHashMap<>(paramsSrc);
        else
            params = Collections.emptyMap();

        for (TemplateHolder holder : holders) {
            if (!holder.append(sb, params))
                break;
        }
        if (!params.isEmpty()) {
            sb.append("?");
            boolean and = false;
            for (Map.Entry param : params.entrySet()) {
                if (and)
                    sb.append("&");
                else
                    and = true;
                sb.append(param.getKey()).append("=").append(param.getValue());
            }
        }
        return sb.toString();
    }

    interface TemplateHolder {
        boolean append(StringBuilder sb, Map params);
    }

    class StringHolder implements TemplateHolder {
        String value;
        byte[] bytes;

        public StringHolder(String value) {
            this.value = value;
            bytes = value.getBytes();
        }


        @Override
        public boolean append(StringBuilder sb, Map params) {
            sb.append(value);
            return true;
        }
    }

    class VariableHolder implements TemplateHolder {
        String param;
        String prefix;
        byte[] prefixBytes;
        boolean optional = false;

        public VariableHolder(String param, String prefix, boolean optional) {
            this.param = param;
            this.optional = optional;
            this.prefix = prefix;
            if (prefix != null)
                prefixBytes = prefix.getBytes();
        }

        @Override
        public boolean append(StringBuilder sb, Map params) {
            Object value = params.remove(param);
            if (optional && value == null)
                return false;
            if (prefix != null)
                sb.append(prefix);
            sb.append(value);
            return true;
        }
    }
}
