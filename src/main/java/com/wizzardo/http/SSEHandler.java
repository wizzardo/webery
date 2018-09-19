package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.readable.ReadableBuilder;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;

import java.nio.charset.StandardCharsets;

public class SSEHandler implements Handler {

    @Override
    public Response handle(Request<HttpConnection, Response> request, Response response) {
        response.async();
        response.appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_TEXT_EVENT_STREAM);

        commitAsyncResponse(request, response);
        request.connection().onDisconnect((c, bb) -> onDisconnect(request));
        onConnect(request);
        return response;
    }

    protected void onConnect(Request request) {
    }

    protected void onDisconnect(Request request) {
    }

    public void sendEvent(Request request, SSEEvent event, ByteBufferProvider byteBufferProvider) {
        request.connection().write(event.toReadableData(), byteBufferProvider);
    }

    protected void commitAsyncResponse(Request request, Response response) {
        HttpConnection connection = request.connection();
        ByteBufferProvider byteBufferProvider = ByteBufferProvider.current();
        response.commit(connection, byteBufferProvider);
        connection.flush(byteBufferProvider);
    }


    public static class SSEEvent {
        protected static final byte[] idPrefix = "id: ".getBytes();
        protected static final byte[] dataPrefix = "data: ".getBytes();
        protected static final byte[] eventPrefix = "event: ".getBytes();
        protected static final byte[] lineSeparator = "\n".getBytes();

        protected String id;
        protected String data;
        protected String event;

        public SSEEvent() {
        }

        public SSEEvent(String data) {
            this.data = data;
        }

        public SSEEvent(String id, String event, String data) {
            this.id = id;
            this.event = event;
            this.data = data;
        }

        public ReadableData toReadableData() {
            ReadableBuilder builder = new ReadableBuilder();
            if (id != null)
                builder.append(idPrefix).append(id.getBytes(StandardCharsets.UTF_8)).append(lineSeparator);
            if (event != null)
                builder.append(eventPrefix).append(event.getBytes(StandardCharsets.UTF_8)).append(lineSeparator);
            if (data != null)
                builder.append(dataPrefix).append(data.getBytes(StandardCharsets.UTF_8)).append(lineSeparator);

            builder.append(lineSeparator);
            return builder;
        }

        public SSEEvent withId(String id) {
            this.id = id;
            return this;
        }

        public SSEEvent withData(String data) {
            this.data = data;
            return this;
        }

        public SSEEvent withEventType(String event) {
            this.event = event;
            return this;
        }
    }

}
