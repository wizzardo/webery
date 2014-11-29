package com.wizzardo.http.response;

/**
 * @author: wizzardo
 * Date: 3/31/14
 */
public enum Status {


    /**
     * Switching Protocols
     */
    _101(101, "Switching Protocols"),

    /**
     * OK
     */
    _200(200, "OK"),

    /**
     * Partial Content
     */
    _206(206, "Partial Content"),

    /**
     * Moved Permanently
     */
    _301(301, "Moved Permanently"),

    /**
     * Found
     */
    _302(302, "Found"),

    /**
     * Not Modified
     */
    _304(304, "Not Modified"),

    /**
     * Bad Request
     */
    _400(400, "Bad Request"),

    /**
     * Not Authorized
     */
    _401(401, "Not Authorized"),

    /**
     * Forbidden
     */
    _403(403, "Forbidden"),

    /**
     * Method Not Allowed
     */
    _405(405, "Method Not Allowed"),

    /**
     * Not Found
     */
    _404(404, "Not Found"),
    /**
     * Requested range not satisfiable
     */
    _416(416, "Requested range not satisfiable"),

    /**
     * Internal Server Error
     */
    _500(500, "Internal Server Error"),;

    public final byte[] header;
    public final int code;

    private Status(int code, String header) {
        this.code = code;
        this.header = ("HTTP/1.1 " + code + " " + header + "\r\n").getBytes();
    }
}
