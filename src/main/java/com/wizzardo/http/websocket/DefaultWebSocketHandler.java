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
public class DefaultWebSocketHandler extends WebSocketHandler {
    protected Set<WebSocketListener> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void onConnect(WebSocketListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onDisconnect(WebSocketListener listener) {
        listeners.remove(listener);
    }

    public void broadcast(String s) {
        Message message = new Message(s);
        for (WebSocketListener listener : listeners) {
            try {
                listener.sendMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
                onDisconnect(listener);
            }
        }
    }

}
