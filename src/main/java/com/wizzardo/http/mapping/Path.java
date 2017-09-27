package com.wizzardo.http.mapping;

import com.wizzardo.http.request.ByteTree;
import com.wizzardo.http.utils.AsciiReader;
import com.wizzardo.http.utils.StringBuilderThreadLocalHolder;
import com.wizzardo.tools.misc.ExceptionDrivenStringBuilder;
import com.wizzardo.tools.reflection.StringReflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by wizzardo on 29.12.14.
 */
public class Path {

    protected static final StringBuilderThreadLocalHolder stringBuilder = new StringBuilderThreadLocalHolder();

    protected List<String> parts;
    protected String path;
    protected boolean endsWithSlash;

    public Path() {
        this(10);
    }

    public Path(int size) {
        parts = new ArrayList<>(size);
    }

    public Path(List<String> parts) {
        this.parts = new ArrayList<>(parts);
    }

    public Path(String... parts) {
        this.parts = new ArrayList<>(Arrays.asList(parts));
    }

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

    public void toString(ExceptionDrivenStringBuilder sb) {
        if (path != null)
            sb.append(path);
        else
            build(sb);
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
        Path path = new Path(parts);
        add(path, part);
        return path;
    }

    protected void add(Path path, String part) {
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
    }

    private String build() {
        return build(stringBuilder.get()).toString();
    }

    private ExceptionDrivenStringBuilder build(ExceptionDrivenStringBuilder sb) {
        for (String part : parts)
            sb.append('/').append(part);

        if (endsWithSlash)
            sb.append('/');

        return sb;
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
        return parse(bytes, offset, limit, byteTree, new Path());
    }

    public static Path parse(byte[] bytes, int offset, int limit, ByteTree byteTree, Path path) {
        if (bytes[offset] != '/')
            throw new IllegalStateException("path must starts with '/'");

        int length = limit - offset;
        int partStart = offset + 1;
        int partHash = 0;
        ByteTree.Node node = getByteTreeRoot(byteTree);

        byte b;
        for (int i = 1; i < length; i++) {
            b = bytes[offset + i];
            if (b == '/') {
                String value = null;
                if (node != null)
                    value = node.getValue();

                if (value == null) {
                    value = AsciiReader.read(bytes, partStart, i - partStart, partHash);
                }

                if (!append(value, path))
                    throw new IllegalStateException("can't parse: " + new String(bytes, offset, length));

                partStart = i + 1;
                partHash = 0;
                node = getByteTreeRoot(byteTree);
            } else {
                partHash = 31 * partHash + (b & 0xff);
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
                value = AsciiReader.read(bytes, partStart, limit - partStart, partHash);
            }

            if (!append(value, path))
                throw new IllegalStateException("can't parse: " + new String(bytes, offset, length));

        } else
            path.endsWithSlash = true;

        return path;
    }

    protected static ByteTree.Node getByteTreeRoot(ByteTree byteTree) {
        return byteTree != null ? byteTree.getRoot() : null;
    }

    protected static boolean append(String part, Path path) {
        if (part.equals("..")) {
            if (path.parts.isEmpty())
                return false;
            path.parts.remove(path.parts.size() - 1);
        } else
            path.parts.add(part);
        return true;
    }

    public void clear() {
        parts.clear();
        path = null;
        endsWithSlash = false;
    }
}
