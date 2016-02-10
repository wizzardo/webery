package com.wizzardo.http;

import com.wizzardo.http.response.Status;

/**
 * Created by wizzardo on 11.02.16.
 */
public class HttpException extends RuntimeException {
    public final Status status;

    public HttpException(Throwable cause, Status status) {
        super(cause);
        this.status = status;
    }

}
