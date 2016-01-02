package com.wizzardo.http;

import com.wizzardo.http.html.HtmlBuilder;
import com.wizzardo.http.html.Tag;
import com.wizzardo.http.mapping.Path;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.RangeResponseHelper;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.misc.DateIso8601;
import com.wizzardo.tools.misc.Unchecked;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;

import static com.wizzardo.http.html.HtmlBuilder.*;

/**
 * @author: wizzardo
 * Date: 19.09.14
 */
public class FileTreeHandler<T extends FileTreeHandler.HandlerContext> implements Handler {
    protected static final int SIZE_GB = 1024 * 1024 * 1024;
    protected static final int SIZE_MB = 1024 * 1024;
    protected static final int SIZE_KB = 1024;
    protected static final String ORDER_ASC = "asc";
    protected static final String ORDER_DESC = "desc";

    private String prefix;
    private String workDirPath;
    private File workDir;
    protected boolean showFolder = true;

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
        Path p = request.path();
        if (request.context() != null)
            p = p.subPath(1);

        String path = p.toString();

        if (!path.startsWith(prefix))
            return response.setStatus(Status._400).setBody("path must starts with prefix '" + prefix + "'. path=" + path);

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
            if (showFolder)
                return response.appendHeader(Header.KV_CONTENT_TYPE_HTML_UTF8).setBody(renderDirectory(request, file));
            else
                return response.setStatus(Status._403).setBody(path + " is forbidden");

        return RangeResponseHelper.makeRangeResponse(request, response, file);
    }

    public FileTreeHandler<T> setShowFolder(boolean showFolder) {
        this.showFolder = showFolder;
        return this;
    }

    protected String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw Unchecked.rethrow(e);
        }
    }

    protected Tag createHeader(File dir, Tag holder) {
        renderFolderPath(dir, holder);
        return holder;
    }

    protected Tag createTableHeader(String path, String sort, String order) {
        return tr()
                .add(th().add(a().href(path + "?sort=name&order=" + ("name".equals(sort) ? (ORDER_DESC.equals(order) ? ORDER_ASC : ORDER_DESC) : ORDER_ASC))
                                .text("Name"))
                )
                .add(th().add(a().href(path + "?sort=modified&order=" + ("modified".equals(sort) ? (ORDER_DESC.equals(order) ? ORDER_ASC : ORDER_DESC) : ORDER_DESC))
                                .text("Last modified"))
                )
                .add(th().add(a().href(path + "?sort=size&order=" + ("size".equals(sort) ? (ORDER_DESC.equals(order) ? ORDER_ASC : ORDER_DESC) : ORDER_DESC))
                                .text("Size"))
                )
                ;
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

    private Tag renderDirectory(Request request, File dir) {
        String path = prefix + getCanonicalPath(dir).substring(workDirPath.length());
        File[] files = dir.listFiles();

        String sort = request.paramWithDefault("sort", "name");
        String order = request.paramWithDefault("order", "asc");

        int orderInt = order.equals("asc") ? 1 : -1;
        int sortInt = sort.equals("modified") ? 3 : sort.equals("size") ? 2 : 1;

        Arrays.sort(files, (o1, o2) -> {
            if (o1.isDirectory() && !o2.isDirectory())
                return -1;

            if (o2.isDirectory() && !o1.isDirectory())
                return 1;

            if (sortInt == 3) {
                int result = Long.compare(o1.lastModified(), o2.lastModified());
                if (result != 0)
                    return result * orderInt;
            }

            if (sortInt == 2) {
                int result = Long.compare(o1.length(), o2.length());
                if (result != 0)
                    return result * orderInt;
            }

            return o1.getName().compareTo(o2.getName()) * orderInt;
        });

        if (!path.endsWith("/"))
            path += '/';

        T handlerContext = createHandlerContext(path, request);
        return render(path, dir, files, sort, order, handlerContext);
    }

    protected Tag render(String path, File dir, File[] files, String sort, String order, T handlerContext) {
        HtmlBuilder html = new HtmlBuilder();
        html.add(header().add(Meta.charset("utf-8").add(title(path))));
        html.add(body()
                        .add(createHeader(dir, h(1)))
                        .add(table()
                                .attr("border", "0")
                                .add(createTableHeader(path, sort, order))
                                .text("\n")
                                .each(files, (file, table) -> {
                                    String url = generateUrl(file, handlerContext);
                                    table.add(tr()
                                                    .add(td().add(a().href(url).text(file.getName() + (file.isDirectory() ? "/" : ""))))
                                                    .add(td().text(DateIso8601.format(new Date(file.lastModified()))))
                                                    .add(td().attr("align", "right").text(formatFileSize(file.length())))
                                    ).text("\n");
                                }))
        );

        return html;
    }

    protected T createHandlerContext(String path, Request request) {
        return (T) new HandlerContext(path);
    }

    protected String generateUrl(File file, T handlerContext) {
        return encodeName(file.getName()) + (file.isDirectory() ? "/" : "");
    }

    private String encodeName(String name) {
        return Unchecked.call(() -> URLEncoder.encode(name, "utf-8").replace("+", "%20"));
    }

    private String decodePath(String path) {
        return Unchecked.call(() -> URLDecoder.decode(path, "utf-8"));
    }

    protected static class HandlerContext {
        protected final String path;

        public HandlerContext(String path) {
            this.path = path;
        }
    }

    protected String formatFileSize(long l) {
        if (l >= SIZE_GB)
            return l / SIZE_GB + "G";

        if (l >= SIZE_MB)
            return l / SIZE_MB + "M";

        if (l >= SIZE_KB)
            return l / SIZE_KB + "K";

        return String.valueOf(l);
    }
}
