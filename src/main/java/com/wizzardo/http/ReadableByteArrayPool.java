package com.wizzardo.http;

import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.tools.misc.pool.*;

import java.io.IOException;
import java.lang.ref.SoftReference;

/**
 * Created by wizzardo on 26/05/16.
 */
public class ReadableByteArrayPool {
    private static final Pool<PooledReadableByteArray> pool = new PoolBuilder<PooledReadableByteArray>()
            .queue(PoolBuilder.createSharedQueueSupplier())
            .supplier(() -> new PooledReadableByteArray(new byte[10240]))
            .holder(SoftHolder::new)
            .resetter(PooledReadableByteArray::reset)
            .build();

    public interface WithHolder<T> {
        void setHolder(Holder<T> holder);
    }

    private static class SoftHolder<T extends WithHolder<T>> implements Holder<T> {
        final Pool<T> pool;
        final SoftReference<T> ref;

        private SoftHolder(Pool<T> pool, T value) {
            this.pool = pool;
            value.setHolder(this);
            ref = new SoftReference<>(value);
        }

        @Override
        public T get() {
            T value = ref.get();
            if (value == null) {
                while (pool.size() > 0) {
                    SoftHolder<T> holder = (SoftHolder<T>) pool.holder();
                    T t = holder.ref.get();
                    if (t != null)
                        return pool.reset(t);
                }

                T t = pool.create();
                new SoftHolder<>(pool, t);
                return t;
            } else {
                pool.reset(value);
            }
            return value;
        }

        @Override
        public void close() {
            if (ref.get() != null)
                pool.release(this);
        }
    }

    public static class PooledReadableByteArray extends ReadableByteArray implements WithHolder<PooledReadableByteArray>{
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

        public void reset() {
            length = bytes.length;
            position = 0;
        }

        @Override
        public void setHolder(Holder<PooledReadableByteArray> holder) {
            this.holder = holder;
        }
    }

    public static PooledReadableByteArray get() {
        return pool.holder().get();
    }
}
