package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.http.request.*;
import com.wizzardo.http.response.Response;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.io.BoyerMoore;
import com.wizzardo.tools.misc.Unchecked;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by wizzardo on 07.01.16.
 */
public class MultipartHandler implements Handler {

    protected Handler handler;
    protected final long limit;

    public MultipartHandler(Handler handler, long limit) {
        this.handler = handler;
        this.limit = limit;
    }

    public MultipartHandler(Handler handler) {
        this(handler, -1);
    }

    @Override
    public String name() {
        return handler.name();
    }

    @Override
    public Response handle(Request request, Response response) throws IOException {
        if (!request.isMultipart())
            return response.status(Status._400);

        long length = request.contentLength();
        if (limit >= 0 && length > limit)
            return response.setStatus(Status._413);

        String boundary = request.header(Header.KEY_CONTENT_TYPE);
        boundary = "--" + boundary.substring(boundary.indexOf("boundary=") + "boundary=".length());
        BlockReader br = new BlockReader(boundary.getBytes(), new MultipartConsumer(entry -> {

            if (entry instanceof MultiPartTextEntry) {
                String value = new String(entry.asBytes());
                MultiValue multiValue = request.params().putIfAbsent(entry.name(), new MultiValue(value));
                if (multiValue != null)
                    multiValue.append(value);
            }
            request.entry(entry.name(), entry);
        }));

        request.connection().setInputListener(createListener((c) -> {
            request.setMultiPartDataPrepared();
            handler.handle(request, response);
            response.commit(c);
            if (!c.isKeepAlive()) {
                c.flush();
                c.setCloseOnFinishWriting(true);
            }
            c.onFinishingHandling();
            clean(request);
        }, length, br));
        response.async();
        return response;
    }

    protected interface OnFinishProcessing {
        void onFinish(HttpConnection c) throws IOException;
    }

    protected interface EntrySetter {
        void set(MultiPartEntry entry);
    }

    protected void clean(Request request) {
        Collection<MultiPartEntry> entries = request.entries();
        for (MultiPartEntry entry : entries) {
            entry.delete();
        }
    }

    protected InputListener<HttpConnection> createListener(OnFinishProcessing onFinishProcessing, long length, BlockReader br) {
        return new InputListener<HttpConnection>() {
            final AtomicLong read = new AtomicLong();

            @Override
            public void onReadyToRead(HttpConnection c) {
                try {
                    if (read.get() != length) {
                        byte[] buffer = Buffer.current().bytes();
                        int r;
                        ByteBufferProvider bufferProvider = ByteBufferProvider.current();
                        try {
                            while ((r = c.read(buffer, bufferProvider)) > 0) {
                                br.process(buffer, 0, r);
                                if (!checkLimit(read.addAndGet(r), c))
                                    return;
                            }
                        } finally {
                            bufferProvider.getBuffer().clear();
                        }
                        if (read.get() != length)
                            return;
                    }
                    onFinishProcessing.onFinish(c);
                } catch (IOException e) {
                    throw Unchecked.rethrow(e);
                }
            }

            @Override
            public void onReady(HttpConnection c) {
                Buffer buffer = Buffer.current();
                int r = buffer.remains();
                br.process(buffer.bytes(), buffer.position(), r);
                buffer.clear();
                if (!checkLimit(read.addAndGet(r), c))
                    return;

                onReadyToRead(c);
            }

            boolean checkLimit(long read, HttpConnection c) {
                if (read > length) {
                    clean(c.request);
                    c.getResponse().setStatus(Status._413).commit(c);
                    c.setCloseOnFinishWriting(true);
                    return false;
                }
                return true;
            }
        };
    }

    protected static class MultipartConsumer implements BlockReader.BytesConsumer {

        EntrySetter entrySetter;
        boolean headerReady;
        String name;
        MultiPartEntry entry;
        BoyerMoore newLine;
        byte[] buffer = new byte[256];
        int bufferLength;
        OutputStream out;
        int rnrn;
        byte[] last = new byte[2];
        int lastBytes = 0;

        public MultipartConsumer(EntrySetter entrySetter) {
            this.entrySetter = entrySetter;
            headerReady = false;
            name = null;
            entry = null;
            newLine = new BoyerMoore("\r\n\r\n".getBytes());
            out = null;
        }

        @Override
        public void consume(boolean end, byte[] b, int offset, int r) {
            try {
                if (end) {
                    if (entry != null) {
                        if (r != 0)
                            if (r == 1) {
                                if (lastBytes == 2)
                                    out.write(last, 0, 1);
                            } else {
                                out.write(last);
                                out.write(b, offset, r - 2);
                            }

                        out.close();
                        entrySetter.set(entry);
                        reset();
                        return;
                    }
                }

                if (!headerReady) {
                    int position = Math.max(0, bufferLength - 4);
                    buffer(b, offset, r);
                    if ((rnrn = newLine.search(buffer, position, bufferLength - position)) == -1) {
                        return;
                    }

                    headerReady = true;
                    String type = new String(buffer, 2, rnrn - 2);

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
                    int off = bufferLength - rnrn - 4 - 2;
                    if (off >= 0) {
                        out.write(buffer, rnrn + 4, off);

                        last[0] = buffer[bufferLength - 2];
                        last[1] = buffer[bufferLength - 1];
                        lastBytes = 2;
                    } else if (off == -1) {
                        last[0] = buffer[bufferLength - 1];
                        lastBytes = 1;
                    } else if (off == -2) {
                        lastBytes = 0;
                    }
                    bufferLength = 0;
                } else {
                    if (r <= 1) {
                        if (r == 0)
                            return;
                        if (lastBytes == 2) {
                            out.write(last, 0, 1);
                            last[0] = last[1];
                            last[1] = b[offset];
                        } else if (lastBytes == 1) {
                            last[1] = b[offset];
                            lastBytes = 2;
                        } else if (lastBytes == 0) {
                            last[0] = b[offset];
                            lastBytes = 1;
                        }
                    } else {
                        out.write(last, 0, lastBytes);
                        out.write(b, offset, r - 2);
                        last[0] = b[offset + r - 2];
                        last[1] = b[offset + r - 1];
                        lastBytes = 2;
                    }
                }
                if (end)
                    consume(true, b, 0, 0);

            } catch (IOException e) {
                throw Unchecked.rethrow(e);
            }
        }

        protected void buffer(byte[] b, int offset, int r) {
            if (bufferLength + r > buffer.length) {
                byte[] bb = new byte[(bufferLength + r) * 3 / 2];
                System.arraycopy(buffer, 0, bb, 0, bufferLength);
                buffer = bb;
            }
            System.arraycopy(b, offset, buffer, bufferLength, r);
            bufferLength += r;
        }

        private void reset() {
            headerReady = false;
            name = null;
            entry = null;
        }
    }
}
