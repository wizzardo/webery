package com.wizzardo.http.filter;

import com.wizzardo.http.Filter;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.misc.pool.*;

import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by wizzardo on 30.06.15.
 */
public class GzipFilter implements Filter {

    protected Pool<ByteArrayOutputStream> buffers = new PoolBuilder<ByteArrayOutputStream>()
            .supplier(ByteArrayOutputStream::new)
            .resetter(ByteArrayOutputStream::reset)
            .build();

    @Override
    public boolean filter(Request request, Response response) {
        return buffers.provide(out -> {
            GZIPOutputStream gout = new GZIPOutputStream(out);
            gout.write(response.getBody());
            gout.close();
            response.setBody(out.toByteArray());
            response.appendHeader(Header.KV_CONTENT_ENCODING_GZIP);
            return true;
        });
    }
}
