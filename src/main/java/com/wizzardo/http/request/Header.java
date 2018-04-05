package com.wizzardo.http.request;

import com.wizzardo.epoll.ByteBufferWrapper;
import com.wizzardo.http.ReadableDirectByteBuffer;

/**
 * @author: moxa
 * Date: 12/2/13
 */
public enum Header {
    KEY_ACCEPT("Accept"),
    KEY_ACCEPT_RANGES("Accept-Ranges"),
    KEY_AUTHORIZATION("Authorization"),
    KEY_ACCEPT_ENCODING("Accept-Encoding"),
    KEY_ACCEPT_LANGUAGE("Accept-Language"),
    KEY_ALLOW("Allow"),
    KEY_CACHE_CONTROL("Cache-Control"),
    KEY_CONNECTION("Connection"),
    KEY_CONTENT_ENCODING("Content-Encoding"),
    KEY_CONTENT_LENGTH("Content-Length"),
    KEY_CONTENT_RANGE("Content-Range"),
    KEY_CONTENT_TYPE("Content-Type"),
    KEY_COOKIE("Cookie"),
    KEY_DATE("Date"),
    KEY_ETAG("ETag"),
    KEY_IF_MODIFIED_SINCE("If-Modified-Since"),
    KEY_IF_NONE_MATCH("If-None-Match"),
    KEY_LAST_MODIFIED("Last-Modified"),
    KEY_LOCATION("Location"),
    KEY_HOST("Host"),
    KEY_ORIGIN("Origin"),
    KEY_PRAGMA("Pragma"),
    KEY_RANGE("Range"),
    KEY_REFERER("Referer"),
    KEY_SEC_WEBSOCKET_KEY("Sec-WebSocket-Key"),
    KEY_SEC_WEBSOCKET_PROTOCOL("Sec-WebSocket-Protocol"),
    KEY_SEC_WEBSOCKET_ACCEPT("Sec-WebSocket-Accept"),
    KEY_SEC_WEBSOCKET_VERSION("Sec-WebSocket-Version"),
    KEY_SERVER("Server"),
    KEY_SET_COOKIE("Set-Cookie"),
    KEY_TRANSFER_ENCODING("Transfer-Encoding"),
    KEY_UPGRADE("Upgrade"),
    KEY_USER_AGENT("User-Agent"),
    KEY_WWW_AUTHENTICATE("WWW-Authenticate"),

    VALUE_APPLICATION_JSON("application/json"),
    VALUE_BYTES("bytes"),
    VALUE_CLOSE("Close"),
    VALUE_CHUNKED("Chunked"),
    VALUE_FORM_URLENCODED("application/x-www-form-urlencoded"),
    VALUE_GZIP("gzip"),
    VALUE_HTML_UTF8("text/html;charset=UTF-8"),
    VALUE_KEEP_ALIVE("Keep-Alive"),
    VALUE_NO_CACHE("no-cache"),
    VALUE_TEXT_PLAIN("text/plain"),
    VALUE_TEXT_XML("text/xml"),
    VALUE_UPGRADE("Upgrade"),
    VALUE_WEBSOCKET("websocket"),

    KV_CONNECTION_CLOSE("Connection: Close\r\n", true),
    KV_CONNECTION_KEEP_ALIVE("Connection: Keep-Alive\r\n", true),
    KV_CONTENT_ENCODING_GZIP("Content-Encoding: gzip\r\n", true),
    KV_CONTENT_TYPE_APPLICATION_JSON("Content-Type: application/json\r\n", true),
    KV_CONTENT_TYPE_APPLICATION_OCTET_STREAM("Content-Type: application/octet-stream\r\n", true),
    KV_CONTENT_TYPE_HTML_UTF8("Content-Type: text/html;charset=UTF-8\r\n", true),
    KV_CONTENT_TYPE_TEXT_PLAIN("Content-Type: text/plain\r\n", true),
    KV_CONTENT_TYPE_TEXT_PLAIN_UTF8("Content-Type: text/plain;charset=UTF-8\r\n", true),
    KV_CONTENT_TYPE_TEXT_XML("Content-Type: text/xml\r\n", true);

    public final String value;
    public final byte[] bytes;
    public final boolean complete;
    public final ReadableDirectByteBuffer buffer;

    Header(String value) {
        this(value, false);
    }

    Header(String value, boolean complete) {
        this.value = value;
        this.complete = complete;
        bytes = value.getBytes();
        buffer = new ReadableDirectByteBuffer(new ByteBufferWrapper(bytes));
    }
}
