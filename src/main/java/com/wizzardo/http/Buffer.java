package com.wizzardo.http;

public interface Buffer {
    byte[] bytes();

    int position();

    void position(int position);

    int limit();

    void limit(int limit);

    /**
     * @return result of casting Thread.currentThread() to {@link Buffer}
     * @throws ClassCastException if current thread doesn't implement {@link Buffer}
     **/
    static Buffer current() {
        return (Buffer) Thread.currentThread();
    }

    int capacity();
}
