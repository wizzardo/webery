package com.wizzardo.http.request;

/**
 * @author: moxa
 * Date: 12/2/13
 */
public enum Header {
    KEY_ACCEPT("Accept"),
    KEY_AUTHORIZATION("Authorization"),
    KEY_ACCEPT_ENCODING("Accept-Encoding"),
    KEY_ACCEPT_LANGUAGE("Accept-Language"),
    KEY_ALLOW("Allow"),
    KEY_CACHE_CONTROL("Cache-Control"),
    KEY_CONNECTION("Connection"),
    KEY_COOKIE("Cookie"),
    KEY_LOCATION("Location"),
    KEY_SET_COOKIE("Set-Cookie"),
    KEY_HOST("Host"),
    KEY_PRAGMA("Pragma"),
    KEY_USER_AGENT("User-Agent"),
    KEY_CONTENT_TYPE("Content-Type"),
    KEY_CONTENT_LENGTH("Content-Length"),
    KEY_RANGE("Range"),
    KEY_CONTENT_RANGE("Content-Range"),
    KEY_ACCEPT_RANGES("Accept-Ranges"),
    KEY_ORIGIN("Origin"),
    KEY_UPGRADE("Upgrade"),
    KEY_SEC_WEBSOCKET_KEY("Sec-WebSocket-Key"),
    KEY_SEC_WEBSOCKET_PROTOCOL("Sec-WebSocket-Protocol"),
    KEY_SEC_WEBSOCKET_ACCEPT("Sec-WebSocket-Accept"),
    KEY_SEC_WEBSOCKET_VERSION("Sec-WebSocket-Version"),
    KEY_WWW_AUTHENTICATE("WWW-Authenticate"),

    VALUE_WEBSOCKET("websocket"),
    VALUE_CONNECTION_CLOSE("Close"),
    VALUE_BYTES("bytes"),
    VALUE_CONNECTION_KEEP_ALIVE("Keep-Alive"),
    VALUE_NO_CACHE("no-cache"),
    VALUE_CONTENT_TYPE_HTML_UTF8("text/html;charset=UTF-8"),
    VALUE_FORM_URLENCODED("application/x-www-form-urlencoded"),
    VALUE_UPGRADE("Upgrade");

    public final String value;
    public final byte[] bytes;

    private Header(String value) {
        this.value = value;
        bytes = value.getBytes();
    }
}
