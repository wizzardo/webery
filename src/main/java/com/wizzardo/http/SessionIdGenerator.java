package com.wizzardo.http;

import com.wizzardo.http.request.Request;

public interface SessionIdGenerator {
    String generate(Request<?,?> request);
}
