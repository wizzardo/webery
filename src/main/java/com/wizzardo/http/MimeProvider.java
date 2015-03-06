package com.wizzardo.http;

import com.wizzardo.http.request.Header;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.http.HttpClient;

import java.io.File;
import java.io.IOException;

/**
 * Created by wizzardo on 06.03.15.
 */
public class MimeProvider {
    private static final String APACHE_MIME_TYPES_URL = "http://svn.apache.org/viewvc/httpd/httpd/branches/2.4.x/docs/conf/mime.types?view=co";

    private UrlMapping<String> types = new UrlMapping<>();

    public MimeProvider() {
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void init() throws IOException {
        String data = HttpClient.createRequest(APACHE_MIME_TYPES_URL).get().asString();
        for (String s : data.split("[\r\n]")) {
            if (s.startsWith("#"))
                continue;

            String[] type = s.split("\\s+");
            for (int i = 1; i < type.length; i++) {
                types.append("*." + type[i], type[0]);
            }
        }
    }

    public String getMimeType(String fileName) {
        return types.get(fileName);
    }

    public Response provideContentType(Response response, File file) {
        String type = getMimeType(file.getName());
        if (type == null)
            return response;

        return response.appendHeader(Header.KEY_CONTENT_TYPE, type);
    }
}
