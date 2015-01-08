package com.wizzardo.http;

import com.wizzardo.http.request.ByteTree;
import com.wizzardo.tools.reflection.StringReflection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wizzardo on 29.12.14.
 */
public class Path {

    private static ByteTree byteTree = new ByteTree();

    private List<String> parts = new ArrayList<>(10);
    private String path;

    public String getPart(int i) {
        if (parts.size() <= i)
            return null;
        return parts.get(i);
    }

    public int size() {
        return parts.size();
    }

    @Override
    public String toString() {
        return path;
    }

    List<String> parts() {
        return parts;
    }

    public static Path parse(byte[] bytes, int offset, int limit) {
        if (bytes[offset] != '/')
            throw new IllegalStateException("path must starts with '/'");

        int length = limit - offset;

        int h = 0;
        int k;

        int partStart = offset + 1;
        int partHash = 0;
        ByteTree.Node node = byteTree.getRoot();

        Path path = new Path();

        byte b;
        char[] data = new char[length];
        data[0] = '/';
        for (int i = 1; i < length; i++) {
            b = bytes[offset + i];
            data[i] = (char) (k = (b & 0xff));
            h = 31 * h + k;
            if (k == '/') {
                String value = null;
                if (node != null)
                    value = node.getValue();

                if (value == null) {
                    char[] part = new char[i - partStart];
                    System.arraycopy(data, partStart, part, 0, i - partStart);
                    value = StringReflection.createString(part, partHash);
                }

                path.parts.add(value);
                partStart = i + 1;
                partHash = 0;
                node = byteTree.getRoot();
            } else {
                partHash = 31 * partHash + k;
                if (node != null)
                    node = node.next(b);
            }
        }
        if (partStart != limit) {
            String value = null;
            if (node != null) {
                value = node.getValue();
            }
            if (value == null) {
                char[] part = new char[limit - partStart];
                System.arraycopy(data, partStart, part, 0, limit - partStart);
                value = StringReflection.createString(part, partHash);
            }
            path.parts.add(value);
        }

        path.path = StringReflection.createString(data, h);

        return path;
    }
}
