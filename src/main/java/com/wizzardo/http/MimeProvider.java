package com.wizzardo.http;

import com.wizzardo.http.mapping.UrlMapping;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by wizzardo on 06.03.15.
 */
public class MimeProvider {
    private UrlMapping<Holder> types = new UrlMapping<>();

    public MimeProvider() {
        try {
            init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void init() throws IOException {
        String data = Unchecked.call(() -> new String(IOTools.bytes(MimeProvider.class.getResourceAsStream("/mime.types")), StandardCharsets.UTF_8));
        for (String s : data.split("[\r\n]")) {
            if (s.startsWith("#"))
                continue;

            String[] type = s.split("\\s+");
            for (int i = 1; i < type.length; i++) {
                types.append("*." + type[i], new Holder(type[0]));
            }
        }
    }

    public String getMimeType(String fileName) {
        Holder holder = types.get(fileName);
        return holder != null ? holder.value : null;
    }

    public Response provideContentType(Response response, File file) {
        String type = getMimeType(file.getName());
        if (type == null)
            return response;

        return response.appendHeader(Header.KEY_CONTENT_TYPE, type);
    }

    private static class Holder implements Named {
        final String value;

        private Holder(String value) {
            this.value = value;
        }
    }
}
