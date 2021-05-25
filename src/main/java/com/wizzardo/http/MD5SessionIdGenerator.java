package com.wizzardo.http;

import com.wizzardo.http.request.Request;
import com.wizzardo.tools.security.MD5;

import java.util.Random;

public class MD5SessionIdGenerator implements SessionIdGenerator {
    private static Random random = new Random();

    @Override
    public String generate(Request<?, ?> request) {
        return MD5.create().update(String.valueOf(random.nextInt(Integer.MAX_VALUE))).asString();
    }
}
