package com.wizzardo.http.request;

import com.wizzardo.tools.io.BoyerMoore;

import java.util.Arrays;

/**
 * Created by wizzardo on 05.01.16.
 */
public class BlockReader {

    protected final byte[] separator;
    protected final int sl;
    protected final BoyerMoore bm;
    protected final BytesConsumer consumer;
    protected byte[] buffer;
    protected int buffered = 0;

    public BlockReader(byte[] separator, BytesConsumer consumer) {
        this.separator = Arrays.copyOf(separator, separator.length);
        this.consumer = consumer;
        bm = new BoyerMoore(separator);
        buffer = new byte[separator.length];
        sl = separator.length;
    }

    public void process(byte[] bytes) {
        process(bytes, 0, bytes.length);
    }

    public void process(byte[] bytes, int offset, int length) {
        if (length <= 0)
            return;

        int start = search(separator, buffer, buffered, bytes, offset, length, bm);
        if (start != -1) {
            if (buffered > 0) {
                if (start <= buffered) {
                    consumer.consume(true, buffer, 0, start);
                    length -= sl + start - offset - buffered;
                    offset = sl + start - buffered;
                    buffered = 0;
                    process(bytes, offset, length);
                } else {
                    consumer.consume(false, buffer, 0, buffered);
                    consumer.consume(true, bytes, offset, start - buffered);

                    length -= sl + start - offset - buffered;
                    offset = sl + start - buffered;
                    buffered = 0;
                    process(bytes, offset, length);
                }
            } else {
                consumer.consume(true, bytes, offset, start - offset);

                length -= sl + start - offset;
                offset = sl + start;
                process(bytes, offset, length);
            }
        } else {
            if (length >= sl) {
                if (buffered > 0)
                    consumer.consume(false, buffer, 0, buffered);
                if (length - sl != 0)
                    consumer.consume(false, bytes, offset, length - sl);

                System.arraycopy(bytes, offset + length - sl, buffer, 0, sl);
                buffered = sl;
            } else {
                int l = buffered + length - sl;
                if (l > 0) {
                    consumer.consume(false, buffer, 0, l);
                    System.arraycopy(buffer, l, buffer, 0, buffered - l);
                    buffered -= l;
                }

                System.arraycopy(bytes, offset, buffer, buffered, length);
                buffered += length;
            }
        }
    }

    public void end() {
        if (buffered == 0)
            return;

        consumer.consume(true, buffer, 0, buffered);
        buffered = 0;
    }

    protected int search(byte[] separator, byte[] buffer, int bufferLength, byte[] bytes, int offset, int length, BoyerMoore bm) {
        if (bufferLength > 0) {
            int start = findPart(buffer, 0, bufferLength, separator);
            if (start != -1 && startsWith(bytes, offset, length, separator, bufferLength - start, separator.length - (bufferLength - start))) {
                return start;
            } else {
                int result = bm.search(bytes, offset, length);
                return result != -1 ? result + bufferLength : -1;
            }
        }
        return bm.search(bytes, offset, length);
    }

    protected boolean startsWith(byte[] src, int srcOffset, int srcLength, byte[] target, int targetOffset, int targetLength) {
        if (srcLength < targetLength)
            return false;

        int limit = Math.min(srcLength, targetLength);
        for (int i = 0; i < limit; i++) {
            if (src[i + srcOffset] != target[i + targetOffset])
                return false;
        }
        return true;
    }

    protected int findPart(byte[] src, int offset, int length, byte[] target) {
        int l = Math.min(length, target.length);
        int o = 0;
        outer:
        while (l != 0) {
            for (int i = 0; i < l; i++) {
                if (src[i + offset + o] != target[i]) {
                    l--;
                    o++;
                    continue outer;
                }
            }
            return o;
        }

        return -1;
    }

    public interface BytesConsumer {
        void consume(boolean end, byte[] bytes, int offset, int length);
    }
}
