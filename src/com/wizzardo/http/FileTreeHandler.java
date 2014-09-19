package com.wizzardo.http;

import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.RangeResponse;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.misc.DateIso8601;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;

/**
 * @author: wizzardo
 * Date: 19.09.14
 */
public class FileTreeHandler implements Handler {
    private File workDir;

    public FileTreeHandler(File workDir) {
        this.workDir = workDir;
    }

    public FileTreeHandler(String workDir) {
        this.workDir = new File(workDir);
    }

    @Override
    public Response handle(Request request) {
//        System.out.println("FileTreeHandler: " + request.path());

        File file = new File(workDir, request.path());
        if (file.getAbsolutePath().length() < workDir.getAbsolutePath().length())
            return new Response().setStatus(Status._403).setBody(request.path() + " is forbidden");

        if (!file.exists())
            return new Response().setStatus(Status._404).setBody(request.path() + " not found");

        if (!file.canRead())
            return new Response().setStatus(Status._403).setBody(request.path() + " is forbidden");

        if (file.isDirectory())
            return renderDirectory(file);

        return new RangeResponse(request, file);
    }

    private Response renderDirectory(File dir) {
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML><HEAD><TITLE>Directory: ");
        String path = dir.getAbsolutePath().substring(workDir.getAbsolutePath().length());
        sb.append(path);
        sb.append("</TITLE></HEAD><BODY>");

        sb.append("<H1>Directory: ").append(path).append("</H1>\n");
        sb.append("<TABLE BORDER=0>\n");

        File[] files = dir.listFiles();

        Arrays.sort(files, (o1, o2) -> {
            if (o1.isDirectory() && o2.isDirectory())
                return o1.getName().compareTo(o2.getName());

            if (o1.isDirectory())
                return -1;

            if (o2.isDirectory())
                return 1;

            return o1.getName().compareTo(o2.getName());
        });

        if (!path.endsWith("/"))
            path += '/';

        for (File file : files) {
            try {
                sb.append("<tr>");

                sb.append("<td>");
                sb.append("<a href=\"").append(path).append(URLEncoder.encode(file.getName(), "utf-8"));
                if (file.isDirectory())
                    sb.append('/');
                sb.append("\">");
                sb.append(file.getName());
                sb.append("</a>");
                sb.append("</td>");

                sb.append("<td align=right>");
                sb.append(file.length()).append(" bytes");
                sb.append("</td>");

                sb.append("<td>");
                sb.append(DateIso8601.format(new Date(file.lastModified())));
                sb.append("</td>");

                sb.append("<tr>\n");
            } catch (UnsupportedEncodingException ignore) {
            }
        }

        sb.append("</TABLE>\n</BODY></HTML>");
        return new Response().setBody(sb.toString());
    }
}
