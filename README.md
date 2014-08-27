Java HTTP-server
=========

server based on my [epoll-lib]

```java
        HttpServer server = new HttpServer(8080) {
            @Override
            public Response handleRequest(Request request) {
                return new Response()
                        .appendHeader(Header.KEY_CONNECTION, Header.VALUE_CONNECTION_KEEP_ALIVE)
                        .appendHeader(Header.KEY_CONTENT_TYPE, Header.VALUE_CONTENT_TYPE_HTML_UTF8)
                        .setBody("It's alive!");
            }
        };
        server.setIoThreadsCount(4);
        server.start();
```


[epoll-lib]:https://github.com/wizzardo/epoll