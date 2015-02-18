package com.wizzardo.http;

import com.wizzardo.http.request.ByteTree;
import com.wizzardo.http.utils.StringBuilderThreadLocalHolder;
import com.wizzardo.tools.misc.ExceptionDrivenStringBuilder;
import com.wizzardo.tools.reflection.StringReflection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wizzardo on 29.12.14.
 */
public class Path {

    protected static final StringBuilderThreadLocalHolder stringBuilder = new StringBuilderThreadLocalHolder();

    private List<String> parts = new ArrayList<>(10);
    private String path;
    private boolean endsWithSlash;

    public String getPart(int i) {
        if (parts.size() <= i)
            return null;
        return parts.get(i);
    }

    public int length() {
        return parts.size();
    }

    @Override
    public String toString() {
        if (path == null)
            path = build();

        return path;
    }

    List<String> parts() {
        return parts;
    }

    public Path subPath(int beginIndex) {
        return subPath(beginIndex, length());
    }

    public Path subPath(int beginIndex, int endIndex) {
        Path path = new Path();
        path.parts = parts.subList(beginIndex, endIndex);
        if (endsWithSlash || endIndex != parts.size())
            path.endsWithSlash = true;
        return path;
    }

    public Path add(String part) {
        Path path = new Path();
        path.parts = new ArrayList<>(parts);
        if (part.isEmpty())
            path.endsWithSlash = true;
        else {
            for (String s : part.split("/")) {
                if (s.isEmpty())
                    continue;
                if (!append(s, path))
                    throw new IllegalStateException("can't parse: " + part);
            }
            if (part.endsWith("/"))
                path.endsWithSlash = true;
        }
        return path;
    }

    private String build() {
        ExceptionDrivenStringBuilder sb = stringBuilder.get();
        for (String part : parts)
            sb.append('/').append(part);

        if (endsWithSlash)
            sb.append('/');
        return sb.toString();
    }

    public boolean isEndsWithSlash() {
        return endsWithSlash;
    }

    public static Path parse(byte[] bytes) {
        return parse(bytes, 0, bytes.length);
    }

    public static Path parse(byte[] bytes, int offset, int limit) {
        return parse(bytes, offset, limit, null);
    }

    public static Path parse(byte[] bytes, int offset, int limit, ByteTree byteTree) {
        if (bytes[offset] != '/')
            throw new IllegalStateException("path must starts with '/'");

        int length = limit - offset;

        int h = '/';
        int k;

        int partStart = offset + 1;
        int partHash = 0;
        ByteTree.Node node = getByteTreeRoot(byteTree);

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

                if (!append(value, path))
                    throw new IllegalStateException("can't parse: " + new String(bytes, offset, length));

                partStart = i + 1;
                partHash = 0;
                node = getByteTreeRoot(byteTree);
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

            if (!append(value, path))
                throw new IllegalStateException("can't parse: " + new String(bytes, offset, length));

        } else
            path.endsWithSlash = true;

        path.path = StringReflection.createString(data, h);

        return path;
    }

    private static ByteTree.Node getByteTreeRoot(ByteTree byteTree) {
        return byteTree != null ? byteTree.getRoot() : null;
    }

    private static boolean append(String part, Path path) {
        if (part.equals("..")) {
            if (path.parts.isEmpty())
                return false;
            path.parts.remove(path.parts.size() - 1);
        } else
            path.parts.add(part);
        return true;
    }
}
