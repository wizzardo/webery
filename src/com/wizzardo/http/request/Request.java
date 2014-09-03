package com.wizzardo.http.request;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.MultiValue;
import com.wizzardo.tools.io.BlockInputStream;
import com.wizzardo.tools.io.ProgressListener;
import com.wizzardo.tools.misc.BoyerMoore;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: wizzardo
 * Date: 7/25/14
 */
public class Request {

    private HttpConnection connection;
    private Map<String, MultiValue> headers;
    private Map<String, MultiValue> params;
    private Map<String, MultiPartEntry> multiPartEntryMap;
    private Method method;
    private String path;
    private String queryString;
    private long contentLength = -1;
    private boolean bodyParsed = false;
    private Boolean multipart;
    private boolean multiPartDataPrepared = false;

    SimpleRequestBody body;

    public static enum Method {
        GET, PUT, POST, DELETE, HEAD, TRACE, OPTIONS, CONNECT, PATCH
    }

    public Request(HttpConnection connection, Map<String, MultiValue> headers, Map<String, MultiValue> params, String method, String path, String queryString) {
        this.connection = connection;
        this.headers = headers;
        this.params = params;
        this.method = Method.valueOf(method);
        this.path = path;
        this.queryString = queryString;
    }

    public String path() {
        return path;
    }

    public SimpleRequestBody getBody() {
        return body;
    }

    public HttpConnection getConnection() {
        return connection;
    }

    public long contentLength() {
        if (contentLength == -1)
            contentLength = headerLong(Header.KEY_CONTENT_LENGTH, 0);
        return contentLength;
    }

    public HttpConnection connection() {
        return connection;
    }

    public Map<String, MultiValue> headers() {
        return headers;
    }

    public String header(Header header) {
        return header == null ? null : header(header.value);
    }

    public long headerLong(Header header) {
        return headerLong(header.value);
    }

    public long headerLong(Header header, long def) {
        return headerLong(header.value, def);
    }

    public long headerLong(String header) throws NumberFormatException {
        return Long.parseLong(header(header));
    }

    public long headerLong(String header, long def) {
        String value = header(header);
        if (value == null)
            return def;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public String header(String key) {
        MultiValue value = headers.get(key);
        return value == null ? null : value.getValue();
    }

    public List<String> headers(String key) {
        MultiValue value = headers.get(key);
        return value == null ? null : value.getValues();
    }

    public Method method() {
        return method;
    }

    public String getQueryString() {
        return queryString;
    }

    public String param(String key) {
        MultiValue value = params().get(key);
        return value == null ? null : value.getValue();
    }

    public List<String> params(String key) {
        MultiValue value = params().get(key);
        return value == null ? null : value.getValues();
    }

    public Map<String, MultiValue> params() {
        if (body != null && !bodyParsed) {
            new RequestReader(headers, params).parseParameters(body.bytes(), 0, (int) contentLength);
            bodyParsed = true;
        }
        return params;
    }

    public byte[] data() {
        return body == null ? null : body.bytes();
    }

    public boolean isMultipart() {
        if (multipart == null)
            multipart = header(Header.KEY_CONTENT_TYPE).startsWith("multipart/form-data;");

        return multipart;
    }

    public MultiPartEntry getMultiPartEntry(String key) {
        if (!multiPartDataPrepared)
            prepareMultiPart();

        if (multiPartEntryMap == null)
            return null;

        return multiPartEntryMap.get(key);
    }

    public void prepareMultiPart() {
        prepareMultiPart(null);
    }

    public InputStream getInputStream() {
        return connection.getInputStream();
    }

    public void prepareMultiPart(final ProgressListener ll) {
        if (isMultipart()) {
            String temp = header(Header.KEY_CONTENT_TYPE);
            long length = headerLong(Header.KEY_CONTENT_LENGTH);
            int r, rnrn;
            byte[] b = new byte[(int) Math.min(length, 50 * 1024)];
            BoyerMoore newLine = new BoyerMoore("\r\n\r\n".getBytes());
            temp = "--" + temp.substring(temp.indexOf("boundary=") + "boundary=".length());
            BlockInputStream br;
            InputStream in = getInputStream();
            br = new BlockInputStream(in, temp.getBytes(), length, ll);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            FileOutputStream fout = null;
            byte[] last = new byte[2];
            boolean headerRead;
            try {
                while (br.hasNext()) {
                    br.next();
                    headerRead = false;
                    String name = null;
                    MultiPartEntry entry = null;
                    while ((r = br.read(b)) != -1) {

                        if (!headerRead && (rnrn = newLine.search(b, 0, r)) != -1) {
                            out.write(b, 0, rnrn);

                            headerRead = true;
                            String type = new String(out.toByteArray());
                            out.reset();

                            name = type.substring(type.indexOf("name=\"") + 6);
                            name = name.substring(0, name.indexOf("\""));

                            String filename;
                            if (type.contains("filename")) {
                                filename = type.substring(type.indexOf("filename=\"") + 10);
                                filename = filename.substring(0, filename.indexOf("\""));
                                entry = new MultiPartEntry(filename);
                                fout = new FileOutputStream(entry.getFile());
                                fout.write(b, rnrn + 4, r - 4 - rnrn - 2);
                                last[0] = b[r - 2];
                                last[1] = b[r - 1];
                            } else
                                out.write(b, rnrn + 4, r - 4 - rnrn);
                        } else if (entry == null)
                            out.write(b, 0, r);
                        else {
                            fout.write(last);
                            fout.write(b, 0, r - 2);
                            last[0] = b[r - 2];
                            last[1] = b[r - 1];
                        }
                    }
                    if (out.size() == 0 && entry == null)
                        continue;

                    if (entry == null) {
                        byte[] bytes = out.toByteArray();

                        String value = new String(bytes, 0, bytes.length - 2);
                        MultiValue multiValue = params.putIfAbsent(name, new MultiValue(value));
                        if (multiValue != null)
                            multiValue.append(value);

                    } else {
                        if (multiPartEntryMap == null)
                            multiPartEntryMap = new HashMap<>();
                        fout.close();
                        multiPartEntryMap.put(name, entry);
                    }
                }
                multiPartDataPrepared = true;
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public static class MultiPartEntry {
        private String filename;
        private File file;

        public MultiPartEntry(String filename) throws IOException {
            this.filename = filename;
            file = File.createTempFile("--MultiPartEntry", "--");
            file.deleteOnExit();
        }

        public String getFilename() {
            return filename;
        }

        public File getFile() {
            return file;
        }
    }
}
