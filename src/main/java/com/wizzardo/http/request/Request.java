package com.wizzardo.http.request;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.HttpDateFormatterHolder;
import com.wizzardo.http.MultiValue;
import com.wizzardo.http.Session;
import com.wizzardo.http.mapping.Path;
import com.wizzardo.http.response.CookieBuilder;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.interfaces.Mapper;
import com.wizzardo.tools.misc.Unchecked;

import java.io.InputStream;
import java.util.*;

/**
 * @author: wizzardo
 * Date: 7/25/14
 */
public class Request<C extends HttpConnection, R extends Response> {
    protected static final int NOT_INITIALISED = -2;
    protected C connection;
    protected R response;
    protected Map<String, MultiValue<String>> headers;
    protected Parameters params;
    protected Map<String, MultiValue<MultiPartEntry>> multiPartEntryMap;
    protected Map<String, String> cookies;
    protected Method method;
    protected Path path;
    protected String protocol;
    protected String queryString;
    protected long contentLength = NOT_INITIALISED;
    protected boolean bodyParsed = false;
    protected Boolean multipart;
    protected boolean multiPartDataPrepared = false;
    protected String sessionId;
    protected boolean ready = false;
    protected volatile State state = State.READING_HEADERS;

    protected RequestBody body;

    public enum Method {
        GET, PUT, POST, DELETE, HEAD, TRACE, OPTIONS, CONNECT, PATCH,

        COPY(Extension.WEBDAV), LOCK(Extension.WEBDAV), MKCOL(Extension.WEBDAV), MOVE(Extension.WEBDAV),
        PROPFIND(Extension.WEBDAV), PROPPATCH(Extension.WEBDAV), UNLOCK(Extension.WEBDAV);

        final Extension[] extensions;

        Method(Extension... extension) {
            this.extensions = extension;
        }
    }

    public enum Extension {
        WEBDAV
    }

    public enum State {
        READING_HEADERS,
        READING_BODY,
        UPGRADED
    }

    public Request(C connection, R response) {
        this.connection = connection;
        this.response = response;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isReady(boolean ready) {
        return this.ready = ready;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Path path() {
        return path;
    }

    public String context() {
        return connection.getServer().getContext();
    }

    public String protocol() {
        return protocol;
    }

    public RequestBody getBody() {
        return body;
    }

    public long contentLength() {
        if (contentLength == NOT_INITIALISED)
            contentLength = headerLong(Header.KEY_CONTENT_LENGTH, -1);
        return contentLength;
    }

    public C connection() {
        return connection;
    }

    public Map<String, MultiValue<String>> headers() {
        return headers;
    }

    public String header(Header header) {
        return header(header, null);
    }

    public String header(Header header, String def) {
        return header == null ? def : header(header.value, def);
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

        return Unchecked.call(() -> HttpDateFormatterHolder.get().parse(s));
    }

    public String header(String key) {
        return header(key, null);
    }

    public String header(String key, String def) {
        if (headers == null)
            return def;
        MultiValue<String> value = headers.get(key);
        return value == null ? def : value.getValue();
    }

    public List<String> headers(String key) {
        MultiValue<String> value = headers.get(key);
        return value == null ? null : value.getValues();
    }

    public Method method() {
        return method;
    }

    public String getQueryString() {
        return queryString;
    }

    public String param(String key) {
        MultiValue<String> value = params().get(key);
        return value == null ? null : value.getValue();
    }

    public String paramWithDefault(String key, String def) {
        String value = param(key);
        return value == null ? def : value;
    }

    public void param(String key, String value) {
        MultiValue<String> mv = params().putIfAbsent(key, new MultiValue<>(value));
        if (mv != null)
            mv.append(value);
    }

    public List<String> params(String key) {
        MultiValue<String> value = params().get(key);
        return value == null ? null : value.getValues();
    }

    public Parameters params() {
        if (body != null && !bodyParsed && header(Header.KEY_CONTENT_TYPE, "").startsWith(Header.VALUE_FORM_URLENCODED.value)) {
            new RequestReader(headers, params).parseParameters(body.bytes(), 0, (int) contentLength);
            bodyParsed = true;
        }
        return params;
    }

    public void reset() {
        ready = false;
        params = null;
        bodyParsed = false;
        body = null;
        contentLength = NOT_INITIALISED;
        multiPartDataPrepared = false;
        multiPartEntryMap = null;
        cookies = null;
        multipart = null;
        sessionId = null;
        state = State.READING_HEADERS;
        response.reset();
    }

    public R response() {
        return response;
    }

    public byte[] data() {
        return body == null ? null : body.bytes();
    }

    public boolean isMultipart() {
        if (multipart == null)
            multipart = header(Header.KEY_CONTENT_TYPE, "").startsWith("multipart/form-data;");

        return multipart;
    }

    public MultiPartEntry entry(String key) {
        if (!multiPartDataPrepared)
            throw new IllegalStateException("To handle multipart request you need to wrap Handler into MultipartHandler");

        if (multiPartEntryMap == null)
            return null;

        MultiValue<MultiPartEntry> multiValue = multiPartEntryMap.get(key);
        return multiValue != null ? multiValue.getValue() : null;
    }

    public <T> T entry(String key, Mapper<MultiPartEntry, T> mapper) {
        MultiPartEntry entry = entry(key);
        if (entry == null)
            return null;

        return mapper.map(entry);
    }

    public void entry(String key, MultiPartEntry entry) {
        if (multiPartEntryMap == null)
            multiPartEntryMap = new HashMap<>();

        MultiValue<MultiPartEntry> multiValue = multiPartEntryMap.computeIfAbsent(key, s -> new MultiValue<>());
        multiValue.append(entry);
    }

    public boolean isMultiPartDataPrepared() {
        return multiPartDataPrepared;
    }

    public void setMultiPartDataPrepared() {
        multiPartDataPrepared = true;
    }

    public Collection<MultiValue<MultiPartEntry>> entries() {
        if (multiPartEntryMap == null)
            return Collections.emptyList();

        return multiPartEntryMap.values();
    }

    public MultiValue<MultiPartEntry> entries(String key) {
        if (multiPartEntryMap == null)
            return null;

        return multiPartEntryMap.get(key);
    }

    public InputStream getInputStream() {
        return connection.getInputStream();
    }

    public boolean isSecured() {
        return connection.isSecured();
    }

    public Session session() {
        return session("/");
    }

    public Session session(String path) {
        Session session = null;
        if (sessionId != null)
            session = Session.find(sessionId);

        if (session != null)
            return session;

        sessionId = cookies().get("JSESSIONID");

        if (sessionId != null)
            session = Session.find(sessionId);

        if (session != null)
            return session;

        session = Session.create(this);
        sessionId = session.getId();

        response().setCookie(new CookieBuilder("JSESSIONID", sessionId).path(path).maxAge(30 * 60).expires(System.currentTimeMillis() + 30 * 60 * 1000));
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
