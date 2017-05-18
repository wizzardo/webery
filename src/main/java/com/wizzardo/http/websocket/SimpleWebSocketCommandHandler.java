package com.wizzardo.http.websocket;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.tools.json.JsonTools;
import javafx.util.Pair;

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
    protected Map<String, Pair<Class<? extends Command>, CommandHandler<? extends T, ? extends Command>>> handlers = new ConcurrentHashMap<>(16, 1f);

    public interface Command {
        default String getName() {
            return this.getClass().getSimpleName();
        }
    }

    public interface CommandHandler<T extends CountedWebSocketListener, C extends Command> {
        void handle(T listener, C command);
    }

    public <C extends Command> void addHandler(Class<C> commandClass, CommandHandler<? extends T, C> handler) {
        handlers.put(commandClass.getSimpleName(), new Pair<>(commandClass, handler));
    }

    @Override
    public void onMessage(T listener, Message message) {
        try {
            byte[] bytes = message.asBytes();

            int[] holder = new int[1];
            int position = readInt(holder, bytes, 0, bytes.length);
            int nameLength = holder[0];
            String commandName;
            if (nameLength != -1) {
                commandName = new String(bytes, position, nameLength);
            } else {
                position = 0;
                nameLength = indexOf((byte) '{', bytes, position, bytes.length);
                commandName = new String(bytes, position, nameLength);
            }
            int offset = position + nameLength;
            Pair<Class<? extends Command>, CommandHandler<? extends T, ? extends Command>> commandHandlerPair = handlers.get(commandName);
            if (commandHandlerPair == null)
                throw new IllegalArgumentException("Unknown command: " + commandName);

            CommandHandler<T, Command> handler = (CommandHandler<T, Command>) commandHandlerPair.getValue();
            Class<? extends Command> commandClass = commandHandlerPair.getKey();
            Command command = JsonTools.parse(bytes, offset, bytes.length - offset, commandClass);
            handler.handle(listener, command);
        } catch (Exception e) {
            onError(e);
        }
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

    protected static int indexOf(byte b, byte[] bytes, int offset, int limit) {
        for (int i = offset; i < limit; i++) {
            if (bytes[i] == b)
                return i;
        }
        return -1;
    }

    protected static int readInt(int[] holder, byte[] bytes, int offset, int limit) {
        int value = 0;
        int i = offset;
        while (i < limit) {
            byte b = bytes[i];
            if (b >= '0' && b <= '9') {
                value = value * 10 + (b - '0');
            } else {
                if (i == offset)
                    holder[0] = -1;
                else
                    holder[0] = value;
                return i;
            }
            i++;
        }

        holder[0] = value;
        return limit;
    }
}
