package com.wizzardo.http;

import com.wizzardo.epoll.*;
import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.framework.RequestContext;
import com.wizzardo.http.framework.RequestHolder;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FallbackServerSocket<T extends HttpConnection> extends EpollServer<T> implements RequestContext {
    protected ServerSocketChannel server;
    protected AbstractHttpServer<T> httpServer;
    protected Selector selector = null;
    protected ByteBufferWrapper byteBufferWrapper = new ByteBufferWrapper(ByteBuffer.allocateDirect(16 * 1024));

    protected RequestHolder requestHolder = new RequestHolder();
    protected String controller;
    protected String action;
    protected String handler;

    public FallbackServerSocket() {
        this(null, 8080, null);
    }

    public FallbackServerSocket(String host, int port, AbstractHttpServer<T> httpServer) {
        this(host, port);
        this.httpServer = httpServer;
    }

    public FallbackServerSocket(String host, int port) {
        this.hostname = normalizeHostname(host);
        this.port = port;
        setPort(port);
        setHostname(host);
    }

    protected void initEpoll(int maxEvents) {
    }

    @Override
    public void setIoThreadsCount(int ioThreadsCount) {
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public RequestHolder getRequestHolder() {
        return requestHolder;
    }

    @Override
    public String controller() {
        return controller;
    }

    @Override
    public String action() {
        return action;
    }

    @Override
    public void setController(String controller) {
        this.controller = controller;
    }

    @Override
    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public void reset() {
        action = null;
        controller = null;
        requestHolder.reset();
    }

    @Override
    public void handler(String name) {
        this.handler = name;
    }

    @Override
    public String handler() {
        return handler;
    }

    public class SelectorConnectionWrapper extends HttpConnection {
        String ip;
        int port;
        SocketChannel channel;
        Queue<ReadableData> sending = new ConcurrentLinkedQueue<ReadableData>();
        private boolean readyToRead;

        public SelectorConnectionWrapper(SocketChannel channel, AbstractHttpServer server) throws IOException {
            super(0, 0, 0, server);
            this.channel = channel;
            InetSocketAddress address = (InetSocketAddress) channel.socket().getRemoteSocketAddress();
            ip = address.getAddress().getHostAddress();
            port = address.getPort();
        }

        @Override
        public String getIp() {
            return ip;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public boolean isReadyToRead() {
            return readyToRead;
        }

        @Override
        public boolean isAlive() {
            return channel.isConnected();
        }

        @Override
        public boolean write(String s, ByteBufferProvider bufferProvider) {
            try {
                return write(s.getBytes("utf-8"), bufferProvider);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean write(byte[] bytes, ByteBufferProvider bufferProvider) {
            return write(bytes, 0, bytes.length, bufferProvider);
        }

        @Override
        public boolean write(byte[] bytes, int offset, int length, ByteBufferProvider bufferProvider) {
            return write(new ReadableByteArray(bytes, offset, length), bufferProvider);
        }

        @Override
        public boolean write(ReadableData readable, ByteBufferProvider bufferProvider) {
            if (sending.isEmpty())
                try {
                    channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, this);
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            sending.add(readable);
            write(bufferProvider);
            return true;
        }

        @Override
        public boolean write(ByteBufferProvider bufferProvider) {
            Queue<ReadableData> queue = this.sending;
            ReadableData readable;
            try {
                while ((readable = queue.peek()) != null) {
                    while (!readable.isComplete() && actualWrite(readable, bufferProvider)) {
                    }
                    if (!readable.isComplete())
                        return false;

                    queue.poll();
                    readable.close();
                    readable.onComplete();
                    onWriteData(readable, !queue.isEmpty());
                }
//                close();

            } catch (Exception e) {
                e.printStackTrace();
                IOTools.close(this);
            }
            return true;
        }

        protected boolean actualWrite(ReadableData readable, ByteBufferProvider bufferProvider) throws IOException {
            ByteBufferWrapper bb = bufferProvider.getBuffer();
            bb.clear();
            ByteBuffer buffer = bb.buffer();
            int r = readable.read(buffer);
            buffer.flip();
            if (r > 0 && isAlive()) {
                int written = channel.write(buffer);
//            System.out.println("write: " + written + " (" + readable.complete() + "/" + readable.length() + ")" + " to " + this);
                if (written != r) {
                    readable.unread(r - written);
                    return false;
                }
                return true;
            }
            return false;
        }

        @Override
        public int read(byte[] b, int offset, int length, ByteBufferProvider bufferProvider) throws IOException {
            ByteBuffer bb = read(length, bufferProvider);
            int r = bb.limit();
            bb.get(b, offset, r);
            return r;
        }

        @Override
        public ByteBuffer read(int length, ByteBufferProvider bufferProvider) throws IOException {
            ByteBufferWrapper bb = bufferProvider.getBuffer();
            bb.clear();
            int l = Math.min(length, bb.limit());
            bb.position(l);
            bb.flip();
            int r = isAlive() ? channel.read(bb.buffer()) : -1;
            if (r > 0)
                bb.position(r);
            bb.flip();
            readyToRead = r == l;
            return bb.buffer();
        }

        @Override
        public void close() {
            setIsAlive(false);
            if (sending != null)
                for (ReadableData data : sending)
                    IOTools.close(data);

            IOTools.close(channel);
        }

        @Override
        public boolean isSecured() {
            return false;
        }

        @Override
        public int read(byte[] bytes, ByteBufferProvider bufferProvider) throws IOException {
            return read(bytes, 0, bytes.length, bufferProvider);
        }

        @Override
        public void setIOThread(IOThread IOThread) {
            throw new IllegalStateException("Not supported yet");
        }
    }


    @Override
    public void run() {
        try {
            started = true;
            selector = Selector.open();
            server = ServerSocketChannel.open();
            server.socket().setReuseAddress(true);
            System.out.println("starting fallback server on " + hostname + ":" + port);
            server.socket().bind(new InetSocketAddress(hostname, port));
            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT);

            while (running) {
//                System.out.println("waiting for events");
                int select = selector.selectNow();
                if (select == 0) {
                    select = selector.select(10);
                    if (select == 0)
                        continue;
                }


                for (Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                    SelectionKey key = i.next();
                    i.remove();
                    if (key.isConnectable()) {
                        ((SocketChannel) key.channel()).finishConnect();
                    }

                    SelectorConnectionWrapper wrapper;
                    if (key.isAcceptable()) {
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.socket().setTcpNoDelay(true);
                        client.register(selector, SelectionKey.OP_READ);
                        wrapper = createConnection(client);
                        key = client.keyFor(selector);
                        key.attach(wrapper);
                    } else {
                        wrapper = (SelectorConnectionWrapper) key.attachment();
                    }

                    if (wrapper == null)
                        continue;

                    if (key.isValid() && key.isReadable()) {
                        onRead((T) wrapper, this);
                    }

                    if (key.isValid() && key.isWritable()) {
                        wrapper.write(this);
                    }
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (IOException e) {
            throw Unchecked.rethrow(e);
        } finally {
            try {
                IOTools.close(selector);
                IOTools.close(server.socket());
                IOTools.close(server);
                Thread.sleep(10);
            } catch (Exception ignored) {
            }
        }
    }

    protected SelectorConnectionWrapper createConnection(SocketChannel client) throws IOException {
        return new SelectorConnectionWrapper(client, httpServer);
    }

    public void onRead(T connection, ByteBufferProvider bufferProvider) throws IOException {
    }

    @Override
    public void setTTL(long milliseconds) {
        System.out.println("setTTL .Not supported yet");
    }

    @Override
    public long getTTL() {
        System.out.println("getTTL .Not supported yet");
        return -1L;
    }

    @Override
    public void close() {
        synchronized (this) {
            if (running) {
                running = false;
                try {
                    join();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    @Override
    public T connect(String host, int port) throws IOException {
        throw new IllegalStateException("Not supported yet");
    }

    @Override
    public ByteBufferWrapper getBuffer() {
        return byteBufferWrapper;
    }

    @Override
    public void loadCertificates(String certFile, String keyFile) {
        throw new IllegalStateException("Not supported yet");
    }

    @Override
    public void loadCertificates(SslConfig sslConfig) {
        throw new IllegalStateException("Not supported yet");
    }

    @Override
    public boolean bind(String host, int port) {
        this.hostname = host;
        this.port = port;
        return true;
    }

    @Override
    public T createConnection(int fd, int ip, int port) {
        return null;
    }

    @Override
    public IOThread createIOThread(int number, int divider) {
        return null;
    }

}