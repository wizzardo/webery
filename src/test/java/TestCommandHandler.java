import com.wizzardo.http.framework.WebApplication;
import com.wizzardo.http.websocket.Message;
import com.wizzardo.http.websocket.SimpleWebSocketClient;
import com.wizzardo.http.websocket.SimpleWebSocketCommandHandler;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by wizzardo on 18/05/17.
 */
public class TestCommandHandler {

    public static class SimpleCommand implements SimpleWebSocketCommandHandler.Command {
        String data;

        @Override
        public String toString() {
            return "SimpleCommand{" +
                    "data='" + data + '\'' +
                    '}';
        }
    }

    @Test
    public void test() throws InterruptedException, IOException, URISyntaxException {
        WebApplication app = new WebApplication();
        SimpleWebSocketCommandHandler<SimpleWebSocketCommandHandler.CountedWebSocketListener> handler = new SimpleWebSocketCommandHandler();
        SimpleWebSocketCommandHandler.CommandHandler<?, SimpleCommand> commandHandler = (listener, command) -> {
            System.out.println(command.data);
            System.out.println(command);
            System.out.println(listener.id);
        };
        handler.addHandler(SimpleCommand.class, (listener, command) -> System.out.println(command.data));
        app.getUrlMapping().append("/ws", handler);
        app.start();
        Thread.sleep(2000);

        SimpleWebSocketClient webSocketClient = new SimpleWebSocketClient("ws://localhost:8080/ws");
        webSocketClient.connectIfNot();
        webSocketClient.send(new Message()
                .append(SimpleCommand.class.getSimpleName())
                .append("{data:qweqwe}")
        );

        Thread.sleep(1000);
    }
}
