package com.wizzardo.http.websocket;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.websocket.extension.SimpleCommandHandler;
import com.wizzardo.http.websocket.extension.SimpleCommandHandler.CommandHandler;
import com.wizzardo.http.websocket.extension.SimpleCommandHandler.CommandPojo;
import com.wizzardo.tools.json.JsonTools;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Created by wizzardo on 18/05/17.
 */

/**
 * Handles commands in format %<b>CommandNameLength</b>%%<b>CommandName</b>%%<b>Command</b>%,<br>
 * for example '13SimpleCommand{}' <br> <br>
 * <b>CommandNameLength</b> is optional
 */
public class SimpleWebSocketCommandHandler<T extends SimpleWebSocketCommandHandler.CountedWebSocketListener> extends DefaultWebSocketHandler<T> {
    protected Map<Integer, T> listeners = new ConcurrentHashMap<>(16, 1f);
    protected SimpleCommandHandler<T> simpleCommandHandler = new SimpleCommandHandler<>(
            (clazz, bytes, offset, length) -> JsonTools.parse(bytes, offset, length, clazz),
            this::onError);

    public <C extends CommandPojo> void addHandler(Class<C> commandClass, CommandHandler<? extends T, C> handler) {
        simpleCommandHandler.addHandler(commandClass, handler);
    }

    @Override
    public void onMessage(T listener, Message message) {
        simpleCommandHandler.onMessage(listener, message);
    }

    protected void onError(Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConnect(T listener) {
        listeners.put(listener.id, listener);
    }

    @Override
    public void onDisconnect(T listener) {
        listeners.remove(listener.id);
    }

    @Override
    protected Collection<T> getListeners() {
        return listeners.values();
    }

    @Override
    protected T createListener(HttpConnection connection, WebSocketHandler handler) {
        return (T) new CountedWebSocketListener(connection, handler);
    }

    public static class CountedWebSocketListener extends WebSocketListener {
        protected static AtomicInteger counter = new AtomicInteger();
        public final int id = counter.incrementAndGet();

        public CountedWebSocketListener(HttpConnection connection, WebSocketHandler webSocketHandler) {
            super(connection, webSocketHandler);
        }
    }
}
