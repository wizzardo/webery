package com.wizzardo.http.response;

import com.wizzardo.epoll.ByteBufferWrapper;
import com.wizzardo.http.ReadableDirectByteBuffer;

/**
 * @author: wizzardo
 * Date: 3/31/14
 */
public enum Status {


    /**
     * Continue
     */
    _100(100, "Continue"),

    /**
     * Switching Protocols
     */
    _101(101, "Switching Protocols"),

    /**
     * OK
     */
    _200(200, "OK"),
    /**
     * Created
     */
    _201(201, "Created"),

    /**
     * Accepted
     */
    _202(202, "Accepted"),

    /**
     * Non-Authoritative Information
     */
    _203(203, "Non-Authoritative Information"),

    /**
     * No Content
     */
    _204(204, "No Content"),

    /**
     * Reset Content
     */
    _205(205, "Reset Content"),

    /**
     * Partial Content
     */
    _206(206, "Partial Content"),

    /**
     * Multiple Choices
     */
    _300(300, "Multiple Choices"),

    /**
     * Moved Permanently
     */
    _301(301, "Moved Permanently"),

    /**
     * Found
     */
    _302(302, "Found"),

    /**
     * See Other
     */
    _303(303, "See Other"),

    /**
     * Not Modified
     */
    _304(304, "Not Modified"),

    /**
     * Use Proxy
     */
    _305(305, "Use Proxy "),

    /**
     * Temporary Redirect
     */
    _307(307, "Temporary Redirect"),

    /**
     * Permanent Redirect
     */
    _308(308, "Permanent Redirect"),

    /**
     * Bad Request
     */
    _400(400, "Bad Request"),

    /**
     * Not Authorized
     */
    _401(401, "Not Authorized"),

    /**
     * Payment Required
     */
    _402(402, "Payment Required"),

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
     * Not Acceptable
     */
    _406(406, "Not Acceptable"),

    /**
     * Request Timeout
     */
    _408(408, "Request Timeout"),

    /**
     * Conflict
     */
    _409(409, "Conflict"),

    /**
     * Gone
     */
    _410(410, "Gone"),

    /**
     * Length Required
     */
    _411(411, "Length Required"),

    /**
     * Precondition Failed
     */
    _412(412, "Precondition Failed"),

    /**
     * Payload Too Large
     */
    _413(413, "Payload Too Large"),

    /**
     * URI Too Long
     */
    _414(414, "URI Too Long"),

    /**
     * Unsupported Media Type
     */
    _415(415, "Unsupported Media Type"),

    /**
     * Requested range not satisfiable
     */
    _416(416, "Requested range not satisfiable"),

    /**
     * Expectation Failed
     */
    _417(417, "Expectation Failed"),

    /**
     * I'm a teapot
     */
    _418(418, "I'm a teapot"),

    /**
     * Misdirected Request
     */
    _421(421, "Misdirected Request"),

    /**
     * Upgrade Required
     */
    _426(426, "Upgrade Required"),

    /**
     * Too Many Requests
     */
    _429(429, "Too Many Requests"),

    /**
     * Request Header Fields Too Large
     */
    _431(431, "Request Header Fields Too Large"),

    /**
     * Internal Server Error
     */
    _500(500, "Internal Server Error"),

    /**
     * Not Implemented
     */
    _501(501, "Not Implemented"),

    /**
     * Bad Gateway
     */
    _502(502, "Bad Gateway"),

    /**
     * Service Unavailable
     */
    _503(503, "Service Unavailable"),

    /**
     * Gateway Timeout
     */
    _504(504, "Gateway Timeout"),

    /**
     * HTTP Version Not Supported
     */
    _505(505, "HTTP Version Not Supported"),;

    public final byte[] bytes;
    public final int code;
    public final String message;
    public final ReadableDirectByteBuffer buffer;

    static {
        //self check
        for (Status status : values()) {
            if (status != valueOf(status.code))
                throw new IllegalStateException("valueOf(code) returns wrong status for code " + status.code);
            if (!status.name().equals("_" + status.code))
                throw new IllegalStateException("status name not valid: " + status);
        }
    }

    Status(int code, String message) {
        this.code = code;
        this.message = message;
        this.bytes = ("HTTP/1.1 " + code + " " + message + "\r\n").getBytes();
        buffer = new ReadableDirectByteBuffer(new ByteBufferWrapper(bytes));
    }

    public static Status valueOf(int status) {
        switch (status) {
            case 100:
                return _100;
            case 101:
                return _101;
            case 200:
                return _200;
            case 201:
                return _201;
            case 202:
                return _202;
            case 203:
                return _203;
            case 204:
                return _204;
            case 205:
                return _205;
            case 206:
                return _206;
            case 300:
                return _300;
            case 301:
                return _301;
            case 302:
                return _302;
            case 303:
                return _303;
            case 304:
                return _304;
            case 305:
                return _305;
            case 307:
                return _307;
            case 308:
                return _308;
            case 400:
                return _400;
            case 401:
                return _401;
            case 402:
                return _402;
            case 403:
                return _403;
            case 404:
                return _404;
            case 405:
                return _405;
            case 406:
                return _406;
            case 408:
                return _408;
            case 409:
                return _409;
            case 410:
                return _410;
            case 411:
                return _411;
            case 412:
                return _412;
            case 413:
                return _413;
            case 414:
                return _414;
            case 415:
                return _415;
            case 416:
                return _416;
            case 417:
                return _417;
            case 418:
                return _418;
            case 421:
                return _421;
            case 426:
                return _426;
            case 429:
                return _429;
            case 431:
                return _431;
            case 500:
                return _500;
            case 501:
                return _501;
            case 502:
                return _502;
            case 503:
                return _503;
            case 504:
                return _504;
            case 505:
                return _505;
        }
        return null;
    }
}
