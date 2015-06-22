package com.wizzardo.http;

import java.util.Map;

/**
 * Created by wizzardo on 12.06.15.
 */
public class ResponseReader extends HttpHeadersReader {

    protected String protocol;
    protected String message;
    protected String status;

    public ResponseReader(Map<String, MultiValue> headers) {
        super(headers);
    }

    public ResponseReader() {
        super();
    }

    @Override
    protected int parseFirstLine(byte[] chars, int offset, int length) {
        if (message != null)
            return offset;

        int l = offset + length;
        for (int i = offset; i < l; i++) {
            byte b = chars[i];
            if (b == ' ') {
                if (protocol == null) {
                    protocol = getValue(chars, offset, i - offset, null);

                    i++;
                    return parseFirstLine(chars, i, length - (i - offset));
                } else if (status == null) {
                    status = getValue(chars, offset, i - offset, null);

                    i++;
                    return parseFirstLine(chars, i, length - (i - offset));
                }

            } else if (b == '\n') {
                message = getValue(chars, offset, i - offset);

                i++;
                return i;
            }
        }
        return offset;

    }

    public String getProtocol() {
        return protocol;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }
}
