package com.wizzardo.httpserver.response;

/**
 * @author: wizzardo
 * Date: 3/31/14
 */
public enum Status {

    _200(200, "OK"),
    _301(301, "Moved Permanently"),
    _302(302, "Found"),
    _304(304, "Not Modified"),
    _400(400, "Bad Request"),
    _403(403, "Forbidden"),
    _404(404, "Not Found"),
    _500(500, "Internal Server Error"),;

    public final byte[] header;
    public final int code;

    private Status(int code, String header) {
        this.code = code;
        this.header = ("HTTP/1.1 " + code + " " + header + "\r\n").getBytes();
    }
}
