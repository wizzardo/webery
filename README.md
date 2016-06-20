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

#### Configuration
```
src/main/resources/Config.groovy
```
```groovy
server {
    host = '0.0.0.0'
    port = 8080
    ioWorkersCount = 1
    ttl = 5 * 60 * 1000
    context = 'myApp'
    basicAuth {
        username = 'user'
        password = 'pass'
        token = true
        tokenTTL = 7 * 24 * 60 * 60 * 1000l
    }

    ssl {
        cert = '/etc/ssl/certs/hostname.crt'
        key = '/etc/ssl/private/hostname.key'
    }
}
//this configuration will be only applied for certain environment
environments {
    dev {
        custom.key = true
    }
    prod {
        custom.key = false
        server.ioWorkersCount = 4
    }
}
```
Configuration stored in Holders
```java
    boolean key = Holders.getConfig().config("custom").get("key", defaulValue);
```


#### Template engine
This framework has it's own template engine, inspired and based on Groovy Server Pages (GSP)
```java
static class AppController extends Controller {
    public Renderer index() {
        model().append("name", params().get("name", "%user name%"));
        return renderView("index");
    }
}
```
Engine will try to render html from template 'resources/views/controller_name/view_name.gsp', in this example:
```
src/main/resources/views/app/index.gsp
```
```html
<html>
   <head>
      <title>Hello</title>
   </head>
   <body>
      Hello, ${name}!
   </body>
 </html>
```