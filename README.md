Java HTTP-server
=========

server based on my [epoll](https://github.com/wizzardo/epoll)

```java
HttpServer<HttpConnection> server = new HttpServer<>(8080);
server.getUrlMapping()
        .append("/", (request, response) -> response.setBody("It's alive!"));
server.start();
```

---

<a name="up"/>
## Framework

- [Initialization](#initialization)
- [Url-mapping](#url-mapping)
- [Dependency injection](#di)
- [Configuration](#configuration)
- [Template engine](#template-engine)
- [Taglib](#taglib)
- [i18n](#i18n)


---

<a name="initialization"/>
#### Initialization [↑](#up)

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

---

<a name="url-mapping"/>
#### Url-mapping [↑](#up)

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

---

<a name="di"/>
#### Dependency injection [↑](#up)
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


##### Raw usage of DI
```java
DependencyFactory.get().register(CustomBean.class, new SingletonDependency<>(CustomBean.class));
CustomBean bean = DependencyFactory.get(CustomBean.class);
```

---

<a name="configuration"/>
#### Configuration [↑](#up)

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

---

<a name="template-engine"/>
#### Template engine [↑](#up)

This framework has it's own template engine, inspired and based on Groovy Server Pages (GSP)
```java
static class AppController extends Controller {
    public Renderer index() {
        model().append("name", params().get("name", "%user name%"));
        return renderView();
    }
}
```
Engine will try to render html from template 'resources/views/controller_name/view_name.gsp', by default 'view_name' is 'action_name':
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

---

<a name="taglib"/>
#### Taglib [↑](#up)

 - [checkBox](#checkBox)
 - [collect](#collect)
 - [createLink](#createLink)
 - [each](#each)
 - [else](#else)
 - [elseif](#elseif)
 - [form](#form)
 - formatBoolean
 - hiddenField
 - if
 - join
 - link
 - message
 - passwordField
 - radio
 - resource
 - set
 - textArea
 - textField
 - while

---

<a name="checkBox"/>
##### checkBox [↑](#taglib)
Generates a checkbox form field.

###### Template:
```html
<g:checkBox name="myCheckbox" value="${true}"/>
<g:checkBox name="myCheckbox" id="myCheckbox_${1}" checked="${true}"/>
```
###### Result:
```html
<input type="checkbox" name="myCheckbox" id="myCheckbox" value="true"/>
<input type="checkbox" name="myCheckbox" id="myCheckbox_1" checked="checked"/>
```
###### Attributes
- name - The name of the checkbox
- value (optional) - The value of the checkbox
- checked (optional) - Expression if evaluates to true sets to checkbox to checked

---

<a name="collect"/>
##### collect [↑](#taglib)
Iterate over each element of the specified collection transforming the result using the expression in the closure

###### Template:
```html
<div>
    <g:collect in="${books}" expr="${it.title}">
        $it<br/>
    </g:collect>
</div>
```

###### Action:
```java
model().append("books", Arrays.asList(new Book("Book one"), new Book("Book two")))
```

###### Book.java:
```java
public class Book {
    public final String title;

    public Book(String title) {
        this.title = title;
    }
}
```

###### Result:
```html
<div>
    Book one
    <br/>
    Book two
    <br/>
</div>
```

###### Attributes
- in - The object to iterative over
- expr - A expression

---
<a name="createLink"/>
##### createLink [↑](#taglib)
Creates a link that can be used where necessary (for example in an href, JavaScript, Ajax call etc.)

Controller:
```java
public class BookController extends Controller {

    public Renderer list() {
        return renderString("list of books");
    }

    public Renderer show() {
        return renderString("some book");
    }
}
```
url-mapping:
```java
app.getUrlMapping()
        .append("/book/list", BookController.class, "list")
        .append("/book/$id", BookController.class, "show");
```

###### Template:
```html
// generates <a href="/book/1">link</a>
<a href="${createLink([controller:'book', action:'show', id: 1])}">link</a>

// generates "/book/show?foo=bar&boo=far"
<g:createLink controller="book" action="show" params="[foo: 'bar', boo: 'far']"/>

// generates "/book/list"
<g:createLink controller="book" action="list" />

// generates "http://localhost:8080/book/list"
<g:createLink controller="book" action="list" absolute="true"/>

// generates "http://ya.ru/book/list"
<g:createLink controller="book" action="list" base="http://ya.ru"/>
```

###### Attributes:
- action (optional) - The name of the action to use in the link; if not specified the current action will be linked
- controller (optional) - The name of the controller to use in the link; if not specified the current controller will be linked
- id (optional) - The id to use in the link
- fragment (optional) - The link fragment (often called anchor tag) to use
- mapping (optional) - The named URL mapping to use, default mapping = controllerName + '.' + actionName
- params (optional) - A Map of request parameters
- absolute (optional) - If true will prefix the link target address with the value of the server.host property from config
- base (optional) - Sets the prefix to be added to the link target address, typically an absolute server URL. This overrides the behaviour of the absolute property if both are specified.

---
<a name="each"/>
##### each [↑](#taglib)
Iterate over each element of the specified collection.

###### Template:
```html
<div>
    <g:each in="[1,2,3]" var="i">
        $i<br/>
    </g:each>
</div>
```
###### Result:
```html
<div>
    1
    <br/>
    2
    <br/>
    3
    <br/>
</div>
```

###### Attributes:
- in - The collection to iterate over
- status (optional) - The name of a variable to store the iteration index in. Starts with 0 and increments for each iteration.
- var (optional) - The name of the item, defaults to "it".

---
<a name="else"/>
##### else [↑](#taglib)
The logical else tag

###### Template:
```html
<g:if test="${false}">
    never happen
</g:if>
<g:else>
    Hello, world!
</g:else>
```

###### Result:
```html
    Hello, world!
```

---
<a name="elseif"/>
##### elseif [↑](#taglib)
The logical elseif tag

###### Template:
```html
<g:if test="${false}">
    never happen
</g:if>
<g:elseif test="${true}">
    Hello, world!
</g:elseif>
```

###### Result:
```html
    Hello, world!
```

###### Attributes:
- test - The expression to test


---
<a name="form"/>
##### form [↑](#taglib)
Creates a form, extends 'createLink' tag

---

<a name="i18n"/>
#### i18n [↑](#up)

```java
    MessageBundle ms = DependencyFactory.getDependency(MessageBundle.class);

    //load message bundle from resources/i18n/messages.properties
    //and lazy load any other language, for example messages_en.properties, messages_fr.properties
    ms.load("messages");

    String foo = ms.get("foo");
    String fooDe = ms.get(Locale.GERMANY,"foo");

    //it also supports templates
    //foobar = {0} {1}
    String foobar = ms.get("foobar", "foo", "bar"); // "foo bar"

```