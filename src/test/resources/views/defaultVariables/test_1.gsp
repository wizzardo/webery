<%@ page import="com.wizzardo.http.request.Request; com.wizzardo.http.response.Response" %>
${((Request) request).path()}
${((Response) response).header("foo")}
${controller}
${action}
${handler}
