package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.http.request.*;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.io.BoyerMoore;
import com.wizzardo.tools.misc.Unchecked;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by wizzardo on 07.01.16.
 */
public class MultipartHandler implements Handler {

    protected Handler handler;

    public MultipartHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public String name() {
        return handler.name();
    }

    @Override
    public Response handle(Request request, Response response) throws IOException {
        if (!request.isMultipart())
            return response.status(Status._400);

        AtomicLong read = new AtomicLong();
        long length = request.contentLength();
        String boundary = request.header(Header.KEY_CONTENT_TYPE);
        boundary = "--" + boundary.substring(boundary.indexOf("boundary=") + "boundary=".length());
        BlockReader br = new BlockReader(boundary.getBytes(), new MultipartConsumer(request));

        request.connection().setInputListener(new InputListener<HttpConnection>() {
            @Override
            public void onReadyToRead(HttpConnection c) {
                try {
                    if (read.get() != length) {
                        byte[] buffer = c.getBuffer();
                        int r;
                        ByteBufferProvider bufferProvider = (ByteBufferProvider) Thread.currentThread();
                        while ((r = c.read(buffer, bufferProvider)) > 0) {
                            br.process(buffer, 0, r);
                            read.addAndGet(r);
                        }
                        if (read.get() != length)
                            return;
                    }
                    handler.handle(request, response);
                    response.commit(c);
                    c.onFinishingHandling();
                } catch (IOException e) {
                    throw Unchecked.rethrow(e);
                }
            }

            @Override
            public void onReady(HttpConnection c) {
                byte[] buffer = c.getBuffer();
                int r = c.getBufferLimit() - c.getBufferPosition();
                br.process(buffer, c.getBufferPosition(), r);
                c.resetBuffer();
                read.addAndGet(r);
                onReadyToRead(c);
            }
        });
        response.async();
        return response;
    }

    private static class MultipartConsumer implements BlockReader.BytesConsumer {

        private final Request request;
        boolean headerReady;
        String name;
        MultiPartEntry entry;
        BoyerMoore newLine;
        ByteArrayOutputStream byteArrayOutputStream;
        OutputStream out;
        int rnrn;
        byte[] last = new byte[2];

        public MultipartConsumer(Request request) {
            this.request = request;
            headerReady = false;
            name = null;
            entry = null;
            newLine = new BoyerMoore("\r\n\r\n".getBytes());
            byteArrayOutputStream = new ByteArrayOutputStream();
            out = null;
        }

        @Override
        public void consume(boolean end, byte[] b, int offset, int r) {
            try {
                if (end) {
                    if (entry != null) {
                        if (r != 0)
                            if (r == 1) {
                                out.write(last, 0, 1);
                            } else {
                                out.write(last);
                                out.write(b, offset, r - 2);
                            }

                        out.close();
                        if (entry instanceof MultiPartTextEntry) {
                            String value = new String(entry.asBytes());
                            MultiValue multiValue = request.params().putIfAbsent(name, new MultiValue(value));
                            if (multiValue != null)
                                multiValue.append(value);
                        }
                        request.entry(entry.name(), entry);
                        reset();
                        return;
                    }
                }

                if (!headerReady) {
                    int read = 0;
                    while ((rnrn = newLine.search(b, Math.max(read - 4, offset), r - read)) == -1) {
                        read += r;
//                    r = br.read(b, read, b.length - read);

                        if ((r == 0 && read == 0)
                                || (r == 4 && b[offset] == '-' && b[offset + 1] == '-' && b[offset + 2] == '\r' && b[offset + 3] == '\n')
                                || (r == 2 && b[offset] == '-' && b[offset + 1] == '-')) {
                            return;
                        }

                        if (r == -1 || read == b.length)
                            throw new IllegalStateException("can't find multipart header end");
                    }
//                    r += read;

                    byteArrayOutputStream.write(b, 2 + offset, rnrn - 2 - offset); //skip \r\n

                    headerReady = true;
                    String type = new String(byteArrayOutputStream.toByteArray());
                    byteArrayOutputStream.reset();

                    name = type.substring(type.indexOf("name=\"") + 6);
                    name = name.substring(0, name.indexOf("\""));

                    if (type.contains("filename")) {
                        String filename = type.substring(type.indexOf("filename=\"") + 10);
                        filename = filename.substring(0, filename.indexOf("\""));
                        entry = new MultiPartFileEntry(name, filename);
                    } else
                        entry = new MultiPartTextEntry(name);

                    for (String header : type.split("\r\n")) {
                        String[] kv = header.split(": ");
                        entry.header(kv[0], kv[1]);
                    }

                    out = entry.outputStream();
                    out.write(b, rnrn + 4, r - (rnrn - offset) - 6);
                    last[0] = b[offset + r - 2];
                    last[1] = b[offset + r - 1];
                } else {
                    if (r <= 1) {
                        if (r == 0)
                            return;
                        out.write(last, 0, 1);
                        last[0] = last[1];
                        last[1] = b[offset];
                    } else {
                        out.write(last);
                        out.write(b, offset, r - 2);
                        last[0] = b[offset + r - 2];
                        last[1] = b[offset + r - 1];
                    }
                }
                if (end)
                    consume(true, b, 0, 0);

            } catch (IOException e) {
                throw Unchecked.rethrow(e);
            }
        }

        private void reset() {
            headerReady = false;
            name = null;
            entry = null;
        }
    }
}
