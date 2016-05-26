package com.wizzardo.http;

import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.tools.misc.pool.*;

import java.io.IOException;

/**
 * Created by wizzardo on 26/05/16.
 */
public class ReadableByteArrayPool {
    private static Pool<PooledReadableByteArray> pool = new ThreadLocalPool<PooledReadableByteArray>() {
        @Override
        public PooledReadableByteArray create() {
            return new PooledReadableByteArray(new byte[10240], this);
        }

        @Override
        protected Holder<PooledReadableByteArray> createHolder(PooledReadableByteArray value) {
            return value.holder;
        }
    };

    public static class PooledReadableByteArray extends ReadableByteArray {
        final Holder<PooledReadableByteArray> holder;

        PooledReadableByteArray(byte[] bytes, Pool<PooledReadableByteArray> pool) {
            super(bytes);
            holder = new SoftHolder<>(pool, this);
        }

        public byte[] bytes() {
            return bytes;
        }

        public void length(int length) {
            this.length = length;
        }

        @Override
        public void close() throws IOException {
            holder.close();
        }
    }

    public static PooledReadableByteArray get() {
        PooledReadableByteArray buffer = pool.get();
        buffer.unread((int) buffer.complete());
        return buffer;
    }
}
