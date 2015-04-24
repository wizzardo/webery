package com.wizzardo.http.template;

import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.tools.collections.CollectionTools;
import com.wizzardo.tools.evaluation.EvalTools;
import com.wizzardo.tools.evaluation.Expression;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: moxa
 * Date: 2/11/13
 */
public class ExecutableTagHolder implements Renderable {
    private CollectionTools.Closure2<ReadableData, Map<String, Object>, Body> closure;
    private Map<String, InnerHolder> attrsRaw;
    private Body body;

    public ExecutableTagHolder(Body body, CollectionTools.Closure2<ReadableData, Map<String, Object>, Body> closure) {
        this.closure = closure;
        attrsRaw = new LinkedHashMap<>(body.attributes().size());
        this.body = body;
        for (Map.Entry<String, String> attr : body.attributes().entrySet()) {
            attrsRaw.put(attr.getKey(), new InnerHolderHelper(attr.getValue()));
        }
    }

    public ReadableData get(Map<String, Object> model) {
        Map<String, Object> attrs = new HashMap<String, Object>(attrsRaw.size() + model.size());
        attrs.putAll(model);
        for (Map.Entry<String, InnerHolder> attr : attrsRaw.entrySet()) {
            attrs.put(attr.getKey(), attr.getValue().get(model));
        }
        return closure.execute(attrs, body);
    }

    static class InnerHolderHelper implements InnerHolder {
        private String string;
        private volatile boolean prepared = false;
        private List<InnerHolder> holders;
        private InnerHolder holder;

        public InnerHolderHelper(String string) {
            this.string = string;
        }

        @Override
        public Object get(Map<String, Object> model) {
            if (!prepared) {
                synchronized (this) {
                    if (!prepared) {
                        try {
                            holders = new ArrayList<>();
                            prepare(string, holders, model);
                            if (holders.size() == 1) {
                                holder = holders.get(0);
                                holders = null;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        prepared = true;
                    }
                }
            }
            try {
                if (holder != null)
                    return holder.get(model);
                else {
                    StringBuilder sb = new StringBuilder();
                    for (InnerHolder in : holders) {
                        sb.append(in.get(model));
                    }
                    return sb.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        private static Pattern p = Pattern.compile("\\$\\{([^\\{\\}]+)\\}|\\$([^\\., -]+)|(\\[.+\\])");

        private void prepare(String s, List<InnerHolder> l, Map<String, Object> model) throws Exception {
            Matcher m = p.matcher(s);
            int last = 0;
            while (m.find()) {
                if (last != m.start())
                    l.add(new InnerStringHolder(s.substring(last, m.start())));
                String exp = m.group(1);
                if (exp == null) {
                    exp = m.group(2);
                }
                if (exp == null) {
                    exp = m.group(3);
                }
                l.add(new InnerExpressionHolder(exp));
                last = m.end();
            }
            if (last != s.length()) {
                l.add(new InnerStringHolder(s.substring(last)));
            }
        }

    }

    private static interface InnerHolder {
        public Object get(Map<String, Object> model);
    }

    private static class InnerStringHolder implements InnerHolder {
        private String string;

        public InnerStringHolder(String string) {
            this.string = string;
        }

        @Override
        public Object get(Map<String, Object> model) {
            return string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    private static class InnerExpressionHolder implements InnerHolder {
        private volatile boolean prepared = false;
        private String string;
        private Expression expression;

        public InnerExpressionHolder(String s) {
            string = s;
        }

        public Object get(Map<String, Object> model) {
            if (!prepared) {
                synchronized (this) {
                    if (!prepared) {
                        try {
                            expression = EvalTools.prepare(string, model);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        prepared = true;
                    }
                }
            }
            try {
                return expression.get(model);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public String toString() {
            return string;
        }
    }
}
