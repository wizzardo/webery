<%@ page import="java.util.Date; com.wizzardo.http.framework.template.ImportTest" %>
<html>
<head>
    <title>Hello World!</title>
</head>

<body>${new ImportTest.Foo()} ${new Date().getTime()/1000}</body>
</html>