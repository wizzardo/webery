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

#### Url-mapping

Controllers and actions could be mapped to static paths or
to something dynamic with variables and wildcards
```java
urlMapping
    .append("/index", AppController.class, "index")
    .append("/books/$id?", AppController.class, "books") // 'id' - is optional
    .append("/optionals/$foo?/$bar?", AppController.class, "optionals") // 'foo' and 'bar' - are optional
    .append("/${foo}-${bar}", AppController.class, "fooBar")
    .append("/any/*", AppController.class, "any")
    .append("*.html", AppController.class, "html")
    ;
```

#### Dependency injection
Framework supports simple dependency injections, to make class or interface injectable simple annotate it with @Injectable.

There are several scopes for it:
- SINGLETON - one instance per jvm, default
- PROTOTYPE - new instance for every injection
- SESSION - one instance per user-session
- REQUEST - new instance every request
- THREAD_LOCAL - one instance per thread

Controllers are stateful so their scope is PROTOTYPE, Services - SINGLETON.
```java
static class AppController extends Controller {
    AppService appService;

    public Renderer index() {
        return renderString(appService.getMessage());
    }
}
```
Framework will also try to find and inject implementation of interfaces and abstract classes.