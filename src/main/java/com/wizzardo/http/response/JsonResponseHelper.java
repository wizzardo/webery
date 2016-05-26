package com.wizzardo.http.response;

import com.wizzardo.epoll.readable.ReadableBuilder;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.http.ReadableByteArrayPool;
import com.wizzardo.http.ReadableByteArrayPool.PooledReadableByteArray;
import com.wizzardo.tools.json.JsonTools;

/**
 * Created by wizzardo on 26/05/16.
 */
public class JsonResponseHelper {

    public static ReadableData renderJson(Object o) {
        PooledReadableByteArray[] holder = new PooledReadableByteArray[1];
        ReadableBuilder builder = new ReadableBuilder();
        JsonTools.serialize(o, () -> {
            PooledReadableByteArray buffer = ReadableByteArrayPool.get();
            holder[0] = buffer;
            return buffer.bytes();
        }, (buffer, offset, length) -> {
            PooledReadableByteArray byteArray = holder[0];
            byteArray.length(length);
            builder.append(byteArray);
        });
        return builder;
    }

}
