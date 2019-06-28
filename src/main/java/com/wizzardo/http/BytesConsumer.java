package com.wizzardo.http;

public interface BytesConsumer {
    void consume(byte[] bytes, int offset, int length);
}
