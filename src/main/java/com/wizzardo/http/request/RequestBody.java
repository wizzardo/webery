package com.wizzardo.http.request;

public interface RequestBody {
    int read(byte[] bytes, int offset, int length);

    int remains();

    boolean isReady();

    byte[] bytes();

    int offset();

    int length();
}
