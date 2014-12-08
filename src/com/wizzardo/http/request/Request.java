package com.wizzardo.http.request;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.HttpDateFormatterHolder;
import com.wizzardo.http.MultiValue;
import com.wizzardo.http.Session;
import com.wizzardo.http.response.CookieBuilder;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.io.BlockInputStream;
import com.wizzardo.tools.io.ProgressListener;
import com.wizzardo.tools.misc.BoyerMoore;
import com.wizzardo.tools.misc.UncheckedThrow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.*;

/**
 * @author: wizzardo
 * Date: 7/25/14
 */
public class Request {
    protected static final int NOT_INITIALISED = -2;

    protected HttpConnection connection;
    protected Map<String, MultiValue> headers;
    protected Map<String, MultiValue> params;
    protected Map<String, MultiPartEntry> multiPartEntryMap;
    protected Map<String, String> cookies;
    protected Method method;
    protected String path;
    protected String protocol;
    protected String queryString;
    protected long contentLength = NOT_INITIALISED;
    protected boolean bodyParsed = false;
    protected Boolean multipart;
    protected boolean multiPartDataPrepared = false;
    protected String sessionId;

    SimpleRequestBody body;

    public static enum Method {
        GET, PUT, POST, DELETE, HEAD, TRACE, OPTIONS, CONNECT, PATCH
    }

    public Request(HttpConnection connection) {
        this.connection = connection;
    }

    public String path() {
        return path;
    }

    public String protocol() {
        return protocol;
    }

    public SimpleRequestBody getBody() {
        return body;
    }

    public HttpConnection getConnection() {
        return connection;
    }

    public long contentLength() {
        if (contentLength == NOT_INITIALISED)
            contentLength = headerLong(Header.KEY_CONTENT_LENGTH, -1);
        return contentLength;
    }

    public HttpConnection connection() {
        return connection;
    }

    public Map<String, MultiValue> headers() {
        return headers;
    }

    public String header(Header header) {
        return header == null ? null : header(header.value);
    }

    public long headerLong(Header header) {
        return headerLong(header.value);
    }

    public long headerLong(Header header, long def) {
        return headerLong(header.value, def);
    }

    public long headerLong(String header) throws NumberFormatException {
        return Long.parseLong(header(header));
    }

    public long headerLong(String header, long def) {
        String value = header(header);
        if (value == null)
            return def;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public Date headerDate(Header header) {
        String s = header(header);
        if (s == null)
            return null;
        try {
            return HttpDateFormatterHolder.get().parse(s);
        } catch (ParseException e) {
            throw UncheckedThrow.rethrow(e);
        }
    }

    public String header(String key) {
        MultiValue value = headers.get(key);
        return value == null ? null : value.getValue();
    }

    public List<String> headers(String key) {
        MultiValue value = headers.get(key);
        return value == null ? null : value.getValues();
    }

    public Method method() {
        return method;
    }

    public String getQueryString() {
        return queryString;
    }

    public String param(String key) {
        MultiValue value = params().get(key);
        return value == null ? null : value.getValue();
    }

    public String paramWithDefault(String key, String def) {
        String value = param(key);
        return value == null ? def : value;
    }

    public void param(String key, String value) {
        MultiValue mv = params().putIfAbsent(key, new MultiValue(value));
        if (mv != null)
            mv.append(value);
    }

    public List<String> params(String key) {
        MultiValue value = params().get(key);
        return value == null ? null : value.getValues();
    }

    public Map<String, MultiValue> params() {
        if (body != null && !bodyParsed) {
            new RequestReader(headers, params).parseParameters(body.bytes(), 0, (int) contentLength);
            bodyParsed = true;
        }
        return params;
    }

    public Response response() {
        return connection.getResponse();
    }

    public byte[] data() {
        return body == null ? null : body.bytes();
    }

    public boolean isMultipart() {
        if (multipart == null)
            multipart = header(Header.KEY_CONTENT_TYPE).startsWith("multipart/form-data;");

        return multipart;
    }

    public MultiPartEntry entry(String key) {
        if (!multiPartDataPrepared)
            prepareMultiPart();

        if (multiPartEntryMap == null)
            return null;

        return multiPartEntryMap.get(key);
    }

    public Collection<MultiPartEntry> entries() {
        return multiPartEntryMap.values();
    }

    public void prepareMultiPart() {
        prepareMultiPart(null);
    }

    public InputStream getInputStream() {
        return connection.getInputStream();
    }

    public void prepareMultiPart(final ProgressListener ll) {
        if (isMultipart()) {
            String boundary = header(Header.KEY_CONTENT_TYPE);
            long length = headerLong(Header.KEY_CONTENT_LENGTH);
            int r, rnrn;
            byte[] b = new byte[(int) Math.min(length, 50 * 1024)];
            BoyerMoore newLine = new BoyerMoore("\r\n\r\n".getBytes());
            boundary = "--" + boundary.substring(boundary.indexOf("boundary=") + "boundary=".length());
            BlockInputStream br;
            InputStream in = getInputStream();
            br = new BlockInputStream(in, boundary.getBytes(), length, ll);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            OutputStream out = null;
            byte[] last = new byte[2];
            boolean headerReady;
            try {
                outer:
                while (br.hasNext()) {
                    br.next();
                    headerReady = false;
                    String name = null;
                    MultiPartEntry entry = null;
                    while ((r = br.read(b)) != -1) {
                        if (!headerReady) {
                            int read = 0;
                            while ((rnrn = newLine.search(b, Math.max(read - 4, 0), read + r - 4)) == -1) {
                                read += r;
                                r = br.read(b, read, b.length - read);

                                if (r == -1 && read == 0)
                                    continue outer;

                                if (r == -1 || read == b.length)
                                    throw new IllegalStateException("can't find multipart header end");
                            }
                            r += read;

                            byteArrayOutputStream.write(b, 2, rnrn - 2); //skip \r\n

                            headerReady = true;
                            String type = new String(byteArrayOutputStream.toByteArray());
                            byteArrayOutputStream.reset();

                            name = type.substring(type.indexOf("name=\"") + 6);
                            name = name.substring(0, name.indexOf("\""));

                            if (type.contains("filename")) {
                                String filename = type.substring(type.indexOf("filename=\"") + 10);
                                filename = filename.substring(0, filename.indexOf("\""));
                                entry = new MultiPartFileEntry(name, filename);
                            } else
                                entry = new MultiPartTextEntry(name);

                            for (String header : type.split("\r\n")) {
                                String[] kv = header.split(": ");
                                entry.header(kv[0], kv[1]);
                            }

                            out = entry.outputStream();
                            out.write(b, rnrn + 4, r - 4 - rnrn - 2);
                            last[0] = b[r - 2];
                            last[1] = b[r - 1];
                        } else {
                            if (r <= 1) {
                                if (r == 0)
                                    continue;
                                out.write(last, 0, 1);
                                last[0] = last[1];
                                last[1] = b[0];
                            } else {
                                out.write(last);
                                out.write(b, 0, r - 2);
                                last[0] = b[r - 2];
                                last[1] = b[r - 1];
                            }
                        }
                    }
                    if (entry == null)
                        continue;

                    if (multiPartEntryMap == null)
                        multiPartEntryMap = new HashMap<>();

                    out.close();
                    multiPartEntryMap.put(name, entry);

                    if (entry instanceof MultiPartTextEntry) {
                        String value = new String(entry.asBytes());
                        MultiValue multiValue = params.putIfAbsent(name, new MultiValue(value));
                        if (multiValue != null)
                            multiValue.append(value);
                    }
                }
                multiPartDataPrepared = true;
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public Session session() {
        if (sessionId != null)
            return Session.find(sessionId);

        sessionId = cookies().get("JSESSIONID");

        if (sessionId != null)
            return Session.find(sessionId);

        Session session = Session.create();
        sessionId = session.getId();

        response().setCookie(new CookieBuilder("JSESSIONID", sessionId).path("/").maxAge(30 * 60).expires(System.currentTimeMillis() + 30 * 60 * 1000));
        return session;
    }

    public Map<String, String> cookies() {
        if (cookies != null)
            return cookies;

        cookies = new LinkedHashMap<>();
        String cookieRaw = header(Header.KEY_COOKIE);
        if (cookieRaw != null && !cookieRaw.isEmpty()) {
            for (String kvRaw : cookieRaw.split("; *")) {
                String[] kv = kvRaw.split("=", 2);
                cookies.put(kv[0].trim(), kv[1].trim());
            }
        }

        return cookies;
    }
}
