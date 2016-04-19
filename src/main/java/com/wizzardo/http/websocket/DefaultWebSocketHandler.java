package com.wizzardo.http.websocket;

import com.wizzardo.http.framework.di.Injectable;

import java.util.Collections;
import java.util.Iterator;
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
        Message message = new Message(s);
        for (T listener : listeners) {
            try {
                listener.sendMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
                onDisconnect(listener);
            }
        }
    }
}
