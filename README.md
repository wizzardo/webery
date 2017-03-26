[![Build Status](https://travis-ci.org/wizzardo/http.svg?branch=master)](https://travis-ci.org/wizzardo/http)
[![codecov](https://codecov.io/gh/wizzardo/http/branch/master/graph/badge.svg)](https://codecov.io/gh/wizzardo/http)

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

- [Installation](#installation)
- [Initialization](#initialization)
- [Building and running](#building-and-running)
- [Url-mapping](#url-mapping)
- [Dependency injection](#di)
- [Configuration](#configuration)
- [Template engine](#template-engine)
- [i18n](#i18n)
- [Taglib](#taglib)


---

<a name="installation"/>

#### Installation [↑](#up)

##### Gradle
```
build.gradle
```
```groovy
apply plugin: 'java'
apply plugin: 'application'

repositories {
    jcenter()
    mavenCentral()
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
    }
}

sourceCompatibility = 1.8
targetCompatibility = 1.8

version = '0.1'

mainClassName = "com.example.MyWebApp"

dependencies {
    compile 'com.wizzardo:http:0.2+'
}

//create a single Jar with all dependencies
task fatJar(type: Jar) {
    manifest {
        attributes(
                "Main-Class": mainClassName
        )
    }
    baseName = project.name + '-all'
    from { configurations.compile.collect { it.isDirectory() ? it : zipTree(it) } }
    with jar
}
```

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
        WebApplication application = new WebApplication(args);
        application.onSetup(app -> {
            app.getUrlMapping()
                    .append("/", AppController.class, "index");
        });
        application.start();
    }
}
```

---

<a name="building-and-running"/>

#### Building and running [↑](#up)

```bash
./gradlew fatJar
java -jar build/libs/MyWebApp-all.jar env=prod profiles.active=profile_A,profile_B
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
    .append("/upload", AppController.class, "upload", Request.Method.POST) // only POST method is allowed
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

To make dependency injection work with implicit dependencies, you need to specify package for scan:
```java
    application.onSetup(app -> {
        DependencyFactory.get(ResourceTools.class)
                .addClasspathFilter(className -> className.startsWith("com.example"));
    });
```

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
        tokenized { // creates mapping with path '/resourceName/*' and respective name for static resources available by token
            resourceName = 'path/to/resource'
        }
    }

    ssl {
        cert = '/etc/ssl/certs/hostname.crt'
        key = '/etc/ssl/private/hostname.key'
    }
    
    session {
        ttl = 30 * 60
    }
    
    resources {
        path = 'public' // load static files from resource folder 'public'
        mapping = '/static'
        cache = {
            enabled = true
            gzip = true
            ttl = -1L
            memoryLimit = 32 * 1024 * 1024L
            maxFileSize = 5 * 1024 * 1024L
        }
    }
    debugOutput = false // dump of all requests and responses to System.out
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

//this configuration will be only applied for certain profiles
profiles {
    profile_A {
        environments {
            dev {
                value = 'value_dev_A'
            }
            prod {
                value = 'value_prod_A'
            }
        }
    }
    profile_B {
        value = 'value_B'
    }
}
```
Configuration stored in Holders:
```java
    boolean key = Holders.getConfig().config("custom").get("key", defaulValue);
```
or it can be mapped to pojo and injected to other objects:
```java
public class CustomConfig implements Configuration {
    public boolean key;

    @Override
    public String prefix() {
        return "custom";
    }
}
```

Configuration is loaded in this order:
 - Default configuration and manifest
 - Config.groovy
 - External configuration (```webApp.onLoadConfiguration(app -> app.loadConfig("MyCustomConfig.groovy"))```)
 - Profiles and environments
 - OS environment variables (```System.getenv()```)
 - Java System properties (```System.getProperties()```)
 - Command line arguments

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

<a name="i18n"/>

#### i18n [↑](#up)

```java
    MessageBundle ms = DependencyFactory.get(MessageBundle.class);

    //load message bundle from resources/i18n/messages.properties
    //and lazy load any other language, for example messages_en.properties, messages_fr.properties
    ms.load("messages");

    String foo = ms.get("foo");
    String fooDe = ms.get(Locale.GERMANY,"foo");

    //it also supports templates
    //foobar = {0} {1}
    String foobar = ms.get("foobar", "foo", "bar"); // "foo bar"

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
 - [formatBoolean](#formatBoolean)
 - [hiddenField](#hiddenField)
 - [if](#if)
 - [join](#join)
 - layoutBody
 - layoutHead
 - layoutTitle
 - [link](#link)
 - [message](#message)
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
<a name="formatBoolean"/>
##### formatBoolean [↑](#taglib)
Outputs the given boolean as the specified text label.

###### Template:
```html
<g:formatBoolean boolean="${myBoolean}" />
<g:formatBoolean boolean="${myBoolean}" true="True!" false="False!" />
```

###### Result:
```html
true
True!
```

###### Attributes:
- boolean - Variable to evaluate
- true (optional) - Output if value is true. If not specified, 'boolean.true' or 'default.boolean.true' is looked up from the Message Source
- false (optional) - Output if value is false. If not specified, 'boolean.false' or 'default.boolean.false' is looked up from the Message Source

---
<a name="hiddenField"/>

##### hiddenField [↑](#taglib)
Creates a input of type 'hidden' (a hidden field).

###### Template:
```html
<g:hiddenField name="myField" value="myValue" />
```

###### Result:
```html
<input type="hidden" name="myField" id="myField" value="myValue"/>
```

###### Attributes:
- name - The name of the text field
- value (optional) - The value of the text field

---
<a name="if"/>

##### if [↑](#taglib)
The logical if tag to switch on an expression.

###### Template:
```html
<g:if test="${true}">
    Hello!
</g:if>
```

###### Result:
```html
    Hello!
```

###### Attributes:
- test - The expression to test

---
<a name="join"/>

##### join [↑](#taglib)
Concatenate the toString() representation of each item in this collection with the given separator.

###### Template:
```html
<g:join in="['Hello', 'World!']" delimiter=", "/>
```

###### Result:
```html
Hello, World!
```

###### Attributes:
- in - The collection to iterate over
- delimiter (optional) - The value of the delimiter to use during the join. Default: ', '

---
<a name="link"/>

##### link [↑](#taglib)
Creates an html anchor tag with the href set based on the specified parameters. Extends 'createLink' tag.

###### Template:
```html
<g:link controller="book" action="show" params="[id: 1]">link</g:link>
```

###### Result:
```html
<a href="/book/1">link</a>
```

###### Attributes:
[same as in createLink tag](#createLink)

---
<a name="message"/>

##### message [↑](#taglib)
Resolves a message from the given code.

###### Template:
```html
// test.message.1.args = test message: {0}
<g:message code="test.message.${1}.args" args="['one']"/>
```

###### Result:
```html
test message: one
```

###### Attributes:
- code - The code to resolve the message for.
- default (optional) - The default message to output if the error or code cannot be found in messages.properties.
- args (optional) - A list of argument values to apply to the message when code is used.
- locale (optional) Override Locale to use instead of the one detected