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
            .queue(PoolBuilder.createThreadLocalQueueSupplier())
            .build();

    @Override
    public boolean filter(Request request, Response response) {
        if ("gzip".equals(response.header(Header.KEY_CONTENT_ENCODING)))
            return true;
        if (response.header(Header.KV_CONTENT_ENCODING_GZIP) != null)
            return true;
        if (response.contentLength() == 0)
            return true;


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
