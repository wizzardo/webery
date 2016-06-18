Java HTTP-server
=========

server based on my [epoll](https://github.com/wizzardo/epoll)

```java
HttpServer<HttpConnection> server = new HttpServer<>(8080);
server.getUrlMapping()
        .append("/", (request, response) -> response.setBody("It's alive!"));
server.start();
```

## Framework

#### Initialization
```java
import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.Environment;
import com.wizzardo.http.framework.WebApplication;
import com.wizzardo.http.framework.template.Renderer;

public class MyWebApp {

    static class AppController extends Controller {
        public Renderer index() {
            return renderString("It's alive!");
        }
    }

    public static void main(String[] args) {
        WebApplication application = new WebApplication();
        application.onSetup(app -> {
            app.getUrlMapping()
                    .append("/", AppController.class, "index");
        });
        application.start();
    }
}
```


