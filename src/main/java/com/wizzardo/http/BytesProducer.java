package com.wizzardo.http;

import java.io.IOException;

public interface BytesProducer {
    void produceTo(BytesConsumer consumer) throws IOException;
}
