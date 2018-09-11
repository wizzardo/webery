package com.wizzardo.http.response;

import com.wizzardo.epoll.readable.ReadableBuilder;
import com.wizzardo.epoll.readable.ReadableByteArray;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.ReadableByteArrayPool;
import com.wizzardo.http.ReadableByteArrayPool.PooledReadableByteArray;
import com.wizzardo.tools.json.JsonTools;

/**
 * Created by wizzardo on 26/05/16.
 */
public class JsonResponseHelper {
    private final static ReadableData EMPTY = new ReadableByteArray(new byte[0]);

    private static class Ref {
        PooledReadableByteArray buffer;
        ReadableBuilder builder;
    }

    public static ReadableData renderJson(Object o) {
        Ref ref = new Ref();
        JsonTools.serialize(o, () -> {
            PooledReadableByteArray buffer = ReadableByteArrayPool.get();
            ref.buffer = buffer;
            return buffer.bytes();
        }, (buffer, offset, length) -> {
            PooledReadableByteArray byteArray = ref.buffer;
            byteArray.length(length);

            if (ref.builder != null || length >= byteArray.bytes().length - 4) {
                if (ref.builder == null)
                    ref.builder = new ReadableBuilder(2);

                ref.builder.append(byteArray);
            }
        });

        if (ref.builder != null)
            return ref.builder;
        if (ref.buffer != null)
            return ref.buffer;
        return EMPTY;
    }

}
