package com.wizzardo.http;

import com.wizzardo.epoll.Connection;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.security.Base64;
import com.wizzardo.tools.security.SHA1;

/**
 * @author: wizzardo
 * Date: 30.09.14
 */
public class WebSocketHandler implements Handler {

    private RawWebSocketHandler rawWebSocketHandler = new RawWebSocketHandler();

    @Override
    public Response handle(Request request, Response response) {
        if (request.method() != Request.Method.GET)
            return response.setStatus(Status._405).setHeader(Header.KEY_ALLOW, Request.Method.GET.name());

        if (!Header.VALUE_WEBSOCKET.value.equals(request.header(Header.KEY_UPGRADE)))
            return response.setStatus(Status._400);

        if (!Header.VALUE_UPGRADE.value.equals(request.header(Header.KEY_CONNECTION)))
            return response.setStatus(Status._400);

        if (request.headerLong(Header.KEY_SEC_WEBSOCKET_VERSION, -1) == 13)
            return response.setStatus(Status._400);

        String key = request.header(Header.KEY_SEC_WEBSOCKET_KEY);

        if (key == null || key.isEmpty())
            return response.setStatus(Status._400);

        key += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"; // websocket magic
        key = Base64.encodeToString(SHA1.getSHA1(key.getBytes()));

        request.connection().upgrade(rawWebSocketHandler);

        return response.setStatus(Status._101)
                .setHeader(Header.KEY_UPGRADE, Header.VALUE_WEBSOCKET)
                .setHeader(Header.KEY_CONNECTION, Header.VALUE_UPGRADE)
                .setHeader(Header.KEY_SEC_WEBSOCKET_ACCEPT, key);
    }

    private static class RawWebSocketHandler implements RawHandler {
        @Override
        public void onData(Connection connection) {
        }

        @Override
        public void onReady(Connection connection) {
        }
    }
}
