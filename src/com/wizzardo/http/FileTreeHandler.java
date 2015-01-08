package com.wizzardo.http;

import com.wizzardo.http.html.HtmlBuilder;
import com.wizzardo.http.html.Tag;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.RangeResponseHelper;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.misc.DateIso8601;
import com.wizzardo.tools.misc.UncheckedThrow;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;

import static com.wizzardo.http.html.HtmlBuilder.*;

/**
 * @author: wizzardo
 * Date: 19.09.14
 */
public class FileTreeHandler implements Handler {
    private String prefix;
    private String workDirPath;
    private File workDir;

    public FileTreeHandler(File workDir, String prefix) {
        if (prefix.endsWith("/"))
            prefix = prefix.substring(0, prefix.length() - 1);
        if (!prefix.isEmpty() && !prefix.startsWith("/"))
            prefix = "/" + prefix;

        this.workDir = workDir;
        this.prefix = prefix;
        workDirPath = getCanonicalPath(workDir);
    }

    public FileTreeHandler(String workDir, String prefix) {
        this(new File(workDir), prefix);
    }

    @Override
    public Response handle(Request request, Response response) {
        String path = request.path();

        if (!path.startsWith(prefix))
            return response.setStatus(Status._400).setBody("path must starts with prefix '" + prefix + "'");

        if (!prefix.isEmpty())
            path = path.substring(prefix.length(), path.length());

        File file = new File(workDir, decodePath(path));
        String canonicalPath = getCanonicalPath(file);

        if (!canonicalPath.startsWith(workDirPath))
            return response.setStatus(Status._403).setBody(path + " is forbidden");

        if (!file.exists())
            return response.setStatus(Status._404).setBody(path + " not found");

        if (!file.canRead())
            return response.setStatus(Status._403).setBody(path + " is forbidden");

        if (file.isDirectory())
            return response.setBody(renderDirectory(file));

        return RangeResponseHelper.makeRangeResponse(request, response, file);
    }

    protected String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw UncheckedThrow.rethrow(e);
        }
    }

    protected Tag createHeader(File dir, Tag holder) {
        renderFolderPath(dir, holder);
        return holder;
    }

    protected StringBuilder renderFolderPath(File dir, Tag holder) {
        if (dir.equals(workDir)) {
            StringBuilder s = new StringBuilder();
            holder.add(a().href("/").text("root: "));
            s.append(prefix).append("/");
            holder.add(a().href(prefix + "/").text(prefix)).text("/");
            return s;
        }

        if (dir.getPath().endsWith("/"))
            return renderFolderPath(dir.getParentFile(), holder);

        StringBuilder path = renderFolderPath(dir.getParentFile(), holder).append(dir.getName()).append("/");
        holder.add(a().href(path.toString()).text(dir.getName())).text("/");
        return path;
    }

    private Tag renderDirectory(File dir) {
        String path = prefix + getCanonicalPath(dir).substring(workDirPath.length());
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

        final String pathHolder = path;

        HtmlBuilder html = new HtmlBuilder();
        html.add(header().add(Meta.charset("utf-8").add(title(path))));
        html.add(body()
                        .add(createHeader(dir, h(1)))
                        .add(table().attr("border", "0").each(files, (file) -> {
                            String url = pathHolder + encodeName(file.getName()) + (file.isDirectory() ? "/" : "");
                            return tr()
                                    .add(td().add(a().href(url).text(file.getName())))
                                    .add(td().attr("align", "right").text(file.length() + " bytes"))
                                    .add(td().text(DateIso8601.format(new Date(file.lastModified()))));
                        }))
        );

        return html;
    }

    private String encodeName(String name) {
        try {
            return URLEncoder.encode(name, "utf-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw UncheckedThrow.rethrow(e);
        }
    }

    private String decodePath(String path) {
        try {
            return URLDecoder.decode(path, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw UncheckedThrow.rethrow(e);
        }
    }
}
