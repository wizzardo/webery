package com.wizzardo.http.websocket;

import com.wizzardo.http.framework.di.Injectable;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wizzardo on 08.12.15.
 */
@Injectable
public class DefaultWebSocketHandler<T extends WebSocketHandler.WebSocketListener> extends WebSocketHandler<T> {
    protected Set<T> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void onConnect(T listener) {
        listeners.add(listener);
    }

    @Override
    public void onDisconnect(T listener) {
        listeners.remove(listener);
    }

    public void broadcast(String s) {
        broadcast(s.getBytes(StandardCharsets.UTF_8));
    }

    public void broadcast(byte[] bytes) {
        broadcast(bytes, 0, bytes.length);
    }

    public void broadcast(byte[] bytes, int offset, int length) {
        Message message = new Message(bytes, offset, length);
        for (T listener : getListeners()) {
            try {
                listener.sendMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
                onDisconnect(listener);
            }
        }
    }

    protected Collection<T> getListeners() {
        return listeners;
    }
}
