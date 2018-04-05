package com.wizzardo.http;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.ByteBufferWrapper;
import com.wizzardo.epoll.EpollCore;
import com.wizzardo.epoll.readable.ReadableData;

import java.nio.ByteBuffer;

public class ReadableDirectByteBuffer extends ReadableData {

    protected ByteBufferWrapper buffer;
    protected int end, position;


    public ReadableDirectByteBuffer(ByteBufferWrapper bufferWrapper) {
        buffer = bufferWrapper;
        end = bufferWrapper.capacity();
        position = 0;
    }

    public ReadableDirectByteBuffer(ByteBuffer buffer) {
        this(new ByteBufferWrapper(buffer));
    }

    @Override
    public ByteBufferWrapper getByteBuffer(ByteBufferProvider bufferProvider) {
        return buffer;
    }

    @Override
    public boolean hasOwnBuffer() {
        return true;
    }

    @Override
    public int read(ByteBuffer bb) {
        int r = Math.min(bb.remaining(), end - position);
        EpollCore.arraycopy(buffer.buffer(), position, bb, bb.position(), r);

        bb.position(r + bb.position());
        position += r;
        return r;
    }

    public int read(ByteBufferWrapper bb) {
        int r = Math.min(bb.remaining(), end - position);
        EpollCore.copy(buffer, position, bb, bb.position(), r);
        bb.position(r + bb.buffer().position());
        position += r;
        return r;
    }

    public static void read(ByteBufferWrapper bb, ReadableDirectByteBuffer s1, ReadableDirectByteBuffer s2, ReadableDirectByteBuffer s3, ReadableDirectByteBuffer s4, ReadableDirectByteBuffer s5) {
        int position = bb.position();
        EpollCore.copyInto(bb, position, s1.buffer, s2.buffer, s3.buffer, s4.buffer, s5.buffer);
        bb.position(s1.end + s2.end + s3.end + s4.end + s5.end + position);
    }

    public static void copy(ByteBufferWrapper dest, ReadableDirectByteBuffer src) {
        int position = dest.position();
        EpollCore.copy(src.buffer, src.position, dest, position, src.end);
        dest.position(src.end + position);
    }

    @Override
    public void unread(int i) {
        if (i < 0)
            throw new IllegalArgumentException("can't unread negative value: " + i);
        if (position - i < 0)
            throw new IllegalArgumentException("can't unread value bigger than offset (" + position + "): " + i);
        position -= i;
    }

    @Override
    public boolean isComplete() {
        return end == position;
    }

    @Override
    public long complete() {
        return position;
    }

    @Override
    public long length() {
        return end;
    }

    @Override
    public long remains() {
        return end - position;
    }

    public ReadableDirectByteBuffer copy() {
        return new ReadableDirectByteBuffer(buffer);
    }

    @Override
    public String toString() {
        return buffer.toString();
    }

    public boolean bufferEquals(ReadableDirectByteBuffer readableDirectByteBuffer) {
        return this.buffer == readableDirectByteBuffer.buffer;
    }
}
