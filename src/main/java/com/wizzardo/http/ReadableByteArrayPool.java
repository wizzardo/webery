package com.wizzardo.http;

import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.tools.misc.pool.*;

import java.io.IOException;
import java.lang.ref.SoftReference;

/**
 * Created by wizzardo on 26/05/16.
 */
public class ReadableByteArrayPool {
    private static Pool<PooledReadableByteArray> pool = new PoolBuilder<PooledReadableByteArray>()
            .queue(PoolBuilder.createThreadLocalQueueSupplier())
            .supplier(() -> new PooledReadableByteArray(new byte[10240]))
            .holder((pool, value) -> value.holder = new SoftHolder<>(pool, value))
            .resetter(it -> it.unread((int) it.complete()))
            .build();

    private static class SoftHolder<T> implements Holder<T> {
        final Pool<T> pool;
        volatile SoftReference<T> ref;

        private SoftHolder(Pool<T> pool, T value) {
            this.pool = pool;
            ref = new SoftReference<>(value);
        }

        @Override
        public T get() {
            T value = ref.get();
            if (value == null) {
                return pool.holder().get();
            } else {
                pool.reset(value);
            }
            return value;
        }

        @Override
        public void close() {
            pool.release(this);
        }
    }

    public static class PooledReadableByteArray extends ReadableByteArray {
        private volatile Holder<PooledReadableByteArray> holder;

        PooledReadableByteArray(byte[] bytes) {
            super(bytes);
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
        return pool.holder().get();
    }
}
