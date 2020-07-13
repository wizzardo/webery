package com.wizzardo.http.framework.template;

public class HtmlTools {

    public static String encodeAsHTML(String s) {
        if (s == null)
            return null;

        StringBuilder sb = null;
        int from = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                if (sb == null)
                    sb = new StringBuilder(s.length() + 32);

                sb.append(s, from, i);
                sb.append("&quot;");
                from = i + 1;
            } else if (c == '<') {
                if (sb == null)
                    sb = new StringBuilder(s.length() + 32);

                sb.append(s, from, i);
                sb.append("&lt;");
                from = i + 1;
            } else if (c == '>') {
                if (sb == null)
                    sb = new StringBuilder(s.length() + 32);

                sb.append(s, from, i);
                sb.append("&gt;");
                from = i + 1;
            } else if (c == '\'') {
                if (sb == null)
                    sb = new StringBuilder(s.length() + 32);

                sb.append(s, from, i);
                sb.append("&apos;");
                from = i + 1;
            } else if (c == '&') {
                if (sb == null)
                    sb = new StringBuilder(s.length() + 32);

                sb.append(s, from, i);
                sb.append("&amp;");
                from = i + 1;
            }
        }
        if (from == 0)
            return s;

        sb.append(s, from, s.length());
        return sb.toString();
    }
}
