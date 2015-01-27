Java HTTP-server
=========

server based on my [epoll-lib]

```java
HttpServer server = new HttpServer(8080);
server.setHandler(new UrlHandler()
        .append("/", (request, response) -> response.setBody("It's alive!")));
server.start();
```


[epoll-lib]:https://github.com/wizzardo/epoll