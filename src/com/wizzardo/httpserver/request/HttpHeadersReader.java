package com.wizzardo.httpserver.request;

/**
 * @author: moxa
 * Date: 12/2/13
 */
public class HttpHeadersReader {

    protected RequestHeaders headers = new RequestHeaders();
    protected String method;
    protected String path;

    protected byte[] buffer;
    protected int r;
    protected byte[] tempKey;
    protected boolean waitForNewLine;

    protected boolean complete = false;


    public int read(byte[] bytes) {
        return read(bytes, 0, bytes.length);
    }

    /**
     * @return int offset in given byte array to request body.
     *         -1 if headers aren't completed
     */
    public int read(byte[] bytes, int offset, int length) {
        if (complete || length == 0)
            return -1;

        int l = offset + length;
        if (method == null || path == null) {
            for (int i = offset; i < l; i++) {
                if (bytes[i] == 32) {// ' ' - space
                    if (method == null)
                        method = getValue(bytes, offset, i - offset);
                    else {
                        path = getValue(bytes, offset, i - offset);
                        waitForNewLine = true;
                    }

                    i++;
                    return read(bytes, i, length - (i - offset));
                }
            }
            putIntoBuffer(bytes, offset, length);
            return -1;
        }
        if (waitForNewLine) {
            for (int i = offset; i < l; i += 2) {
                byte ch = bytes[i];
                if (ch == 10) {   // \n
                    if (i > offset) {
                        if (bytes[i - 1] == 13) {  // \r
                            waitForNewLine = false;
                            i++;
                            if (tempKey != null) {
//                                    headers.put(tempKey, getValue(bytes, offset, i - offset - 1, 1));
//                                    headers.put(tempKey, getValueBytes(bytes, offset, i - offset - 1, 1));
                                headers.put(tempKey, bytes, offset, i - offset - 2, buffer, r);
                                tempKey = null;
                            }
                            r = 0;

                            return read(bytes, i, length - (i - offset));
                        }
                    } else if (i == offset && r > 0 && buffer[r - 1] == 13) {
                        waitForNewLine = false;
                        i++;
                        if (tempKey != null) {
//                                headers.put(tempKey, getValue(bytes, offset, i - offset - 1, 1));
//                                headers.put(tempKey, getValueBytes(bytes, offset, i - offset - 1, 1));
                            headers.put(tempKey, bytes, offset + 1, i - offset - 2, buffer, r);
                            tempKey = null;
                        }
                        r = 0;

                        return read(bytes, i, length - (i - offset));
                    }
                } else if (ch == 13 && ++i < l && bytes[i] == 10) {
                    waitForNewLine = false;
                    i++;
                    if (tempKey != null) {
//                            headers.put(tempKey, getValue(bytes, offset, i - offset - 1, 1));
//                            headers.put(tempKey, getValueBytes(bytes, offset, i - offset - 1, 1));
                        headers.put(tempKey, bytes, offset, i - offset - 2, buffer, r);
                        tempKey = null;
                    }
                    r = 0;

                    return read(bytes, i, length - (i - offset));
                }
            }
            putIntoBuffer(bytes, offset, length);
            return -1;
        }


        if (length >= 2) {
            if (bytes[offset] == 13 && bytes[offset + 1] == 10) {
                complete = true;
                return offset + 2;
            }
        }
        if (bytes[offset] == 10 && r == 1 && buffer[0] == 13) {
            complete = true;
            return offset + 1;
        }

        for (int i = offset; i < l; i++) {
            byte ch = bytes[i];
            if (ch == 58) { // 58 = :
//                    tempKey = getValue(bytes, offset, i - offset);
                tempKey = getValueBytes(bytes, offset, i - offset, 0);
                waitForNewLine = true;

                i++;
                return read(bytes, i, length - (i - offset));
            }
        }
        putIntoBuffer(bytes, offset, length);

        return -1;
    }

    private void putIntoBuffer(byte[] bytes, int offset, int length) {
        if (buffer == null) {
            buffer = new byte[length];

        } else if (buffer.length - r < length) {
            byte[] b = new byte[r + length];
            System.arraycopy(buffer, 0, b, 0, r);
            buffer = b;
        }

        System.arraycopy(bytes, offset, buffer, r, length);
        r += length;
    }

    private String getValue(byte[] bytes, int offset, int length) {
        return getValue(bytes, offset, length, 0);
    }

    private String getValue(byte[] bytes, int offset, int length, int leftShift) {
        if (buffer != null && r > 0) {
            byte[] b = new byte[r + length - leftShift];
            if (length < 0)
                System.arraycopy(buffer, leftShift, b, 0, r - leftShift + length);
            else
                System.arraycopy(buffer, leftShift, b, 0, r - leftShift);

            if (length > 0)
                System.arraycopy(bytes, offset, b, r, length);
            r = 0;
            return new String(b);
        } else {
            return new String(bytes, offset + leftShift, length - leftShift);
        }
    }

    private byte[] getValueBytes(byte[] bytes, int offset, int length, int leftShift) {
        if (buffer != null && r > 0) {
            byte[] b = new byte[r + length - leftShift];
            if (length < 0)
                System.arraycopy(buffer, leftShift, b, 0, r - leftShift + length);
            else
                System.arraycopy(buffer, leftShift, b, 0, r - leftShift);

            if (length > 0)
                System.arraycopy(bytes, offset, b, r, length);
            r = 0;
            return b;
        } else {
            byte[] b = new byte[length - leftShift];
            System.arraycopy(bytes, offset + leftShift, b, 0, length - leftShift);
            return b;
        }
    }

    public RequestHeaders getHeaders() {
        return headers;
    }
}