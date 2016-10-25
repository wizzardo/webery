package com.wizzardo.http.filter;

import com.wizzardo.http.Filter;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.Response;
import com.wizzardo.tools.misc.pool.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

/**
 * Created by wizzardo on 30.06.15.
 */
public class GzipFilter implements Filter {

    protected final int compressionLevel;
    protected final Pool<ByteArrayOutputStream> buffers = new PoolBuilder<ByteArrayOutputStream>()
            .supplier(ByteArrayOutputStream::new)
            .resetter(ByteArrayOutputStream::reset)
            .queue(PoolBuilder.createThreadLocalQueueSupplier())
            .build();

    public GzipFilter() {
        this(Deflater.BEST_SPEED);
    }

    public GzipFilter(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }


    @Override
    public boolean filter(Request request, Response response) {
        if ("gzip".equals(response.header(Header.KEY_CONTENT_ENCODING)))
            return true;
        if (response.header(Header.KV_CONTENT_ENCODING_GZIP) != null)
            return true;
        if (response.contentLength() == 0)
            return true;


        return buffers.provide(out -> {
            GZIPOutputStream gout = new GZIPOutputStream(out, compressionLevel);
            gout.write(response.getBody());
            gout.close();
            response.setBody(out.toByteArray());
            response.appendHeader(Header.KV_CONTENT_ENCODING_GZIP);
            return true;
        });
    }

    static class GZIPOutputStream extends java.util.zip.GZIPOutputStream {
        public GZIPOutputStream(OutputStream out, int compressionLevel) throws IOException {
            super(out);
            def.setLevel(compressionLevel);
        }
    }
}
