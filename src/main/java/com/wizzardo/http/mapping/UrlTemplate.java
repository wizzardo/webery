package com.wizzardo.http.mapping;

import com.wizzardo.tools.misc.Unchecked;

import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wizzardo on 16.04.15.
 */
public class UrlTemplate {
    private TemplatesHolder holder;
    private List<TemplateHolder> holders = new ArrayList<TemplateHolder>();
    private static Pattern variables = Pattern.compile("(/)?\\$\\{?([a-zA-Z_]+[\\w]*)\\}?\\??");

    public UrlTemplate(TemplatesHolder holder, String url) {
        this.holder = holder;

        if (url.endsWith("/*"))
            url = url.substring(0, url.length() - 2);
        else if (url.endsWith("*"))
            url = url.substring(0, url.length() - 1);

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
        return getRelativeUrl(params, null, new StringBuilder(base));
    }

    public String getAbsoluteUrl() {
        return getRelativeUrl(null, null, getStringBuilderWithBase());
    }

    public String getAbsoluteUrl(Map<String, Object> params) {
        return getRelativeUrl(params, null, getStringBuilderWithBase());
    }

    public String getAbsoluteUrl(Map<String, Object> params, String suffix) {
        return getRelativeUrl(params, suffix, getStringBuilderWithBase());
    }

    public String getAbsoluteUrl(String suffix) {
        return getRelativeUrl(null, suffix, getStringBuilderWithBase());
    }

    protected StringBuilder getStringBuilderWithBase() {
        return new StringBuilder(holder.base);
    }

    public String getRelativeUrl() {
        return getRelativeUrl(null, null, null);
    }

    public String getRelativeUrl(Map<String, Object> params) {
        return getRelativeUrl(params, null, null);
    }

    public String getRelativeUrl(Map<String, Object> params, String suffix) {
        return getRelativeUrl(params, suffix, null);
    }

    public String getRelativeUrl(String suffix) {
        return getRelativeUrl(null, suffix, null);
    }

    public String getRelativeUrl(Map<String, ?> paramsSrc, String suffix, StringBuilder sb) {
        if (sb == null)
            sb = new StringBuilder();

        if (holder.context != null)
            sb.append('/').append(holder.context);

        Map<String, Object> params;

        if (paramsSrc != null)
            params = new LinkedHashMap<>(paramsSrc);
        else
            params = Collections.emptyMap();

        for (TemplateHolder holder : holders) {
            if (!holder.append(sb, params))
                break;
        }

        if (suffix != null)
            sb.append(suffix);

        if (!params.isEmpty()) {
            sb.append("?");
            boolean and = false;
            for (Map.Entry param : params.entrySet()) {
                if (and)
                    sb.append("&");
                else
                    and = true;
                sb.append(param.getKey())
                        .append("=")
                        .append(Unchecked.call(() -> URLEncoder.encode(String.valueOf(param.getValue()), "utf-8")));
            }
        }
        return sb.toString();
    }

    interface TemplateHolder {
        boolean append(StringBuilder sb, Map params);
    }

    class StringHolder implements TemplateHolder {
        String value;

        public StringHolder(String value) {
            this.value = value;
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
        boolean optional = false;

        public VariableHolder(String param, String prefix, boolean optional) {
            this.param = param;
            this.optional = optional;
            this.prefix = prefix;
        }

        @Override
        public boolean append(StringBuilder sb, Map params) {
            Object value = params.remove(param);
            if (optional && value == null)
                return false;
            if (prefix != null)
                sb.append(prefix);
            sb.append(Unchecked.call(() -> URLEncoder.encode(String.valueOf(value), "utf-8")));
            return true;
        }
    }
}
