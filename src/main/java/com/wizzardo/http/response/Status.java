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
     * Not Found
     */
    _404(404, "Not Found"),

    /**
     * Method Not Allowed
     */
    _405(405, "Method Not Allowed"),

    /**
     * Requested range not satisfiable
     */
    _416(416, "Requested range not satisfiable"),

    /**
     * Internal Server Error
     */
    _500(500, "Internal Server Error"),;

    public final byte[] bytes;
    public final int code;
    public final String message;

    static {
        //self check
        for (Status status : values()) {
            if (status != valueOf(status.code))
                throw new IllegalStateException("valueOf(code) returns wrong status for code " + status.code);
        }
    }

    Status(int code, String message) {
        this.code = code;
        this.message = message;
        this.bytes = ("HTTP/1.1 " + code + " " + message + "\r\n").getBytes();
    }

    public static Status valueOf(int status) {
        switch (status) {
            case 101:
                return _101;
            case 200:
                return _200;
            case 206:
                return _206;
            case 301:
                return _301;
            case 302:
                return _302;
            case 304:
                return _304;
            case 400:
                return _400;
            case 401:
                return _401;
            case 403:
                return _403;
            case 404:
                return _404;
            case 405:
                return _405;
            case 416:
                return _416;
            case 500:
                return _500;
        }
        return null;
    }
}
