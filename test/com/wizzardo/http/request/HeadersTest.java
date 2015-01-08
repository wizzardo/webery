package com.wizzardo.http.request;

import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.security.MD5;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

/**
 * @author: moxa
 * Date: 12/2/13
 */
public class HeadersTest {

    static interface Checker {
        public void check(RequestReader reader);
    }

    @Test
    public void readerTest() {
        String src;
        Checker checker;

        src = "GET /http/?foo=bar HTTP/1.1\r\n" +
                "Host: example.com\r\n" +
                "Connection: keep-alive\r\n" +
                "Cache-Control: no-cache\r\n" +
                "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n" +
                "Pragma: no-cache\r\n" +
                "User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.114 Safari/537.36\r\n" +
                "Accept-Encoding: gzip,deflate,sdch\r\n" +
                "Accept-Language: en-US,en;q=0.8,ru;q=0.6\r\n" +
                "Cookie: JSESSIONID=1dt8eiw5zc9t4j2o9asxcgmzq; __utma=107222046.2138525965.1372169768.1372169768.1372685422.2; __utmz=107222046.1372169768.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)\r\n" +
                "\r\nololo data foo bar";
        checker = hhr -> {
            Assert.assertEquals("GET", hhr.method);
            Assert.assertEquals("/http/", hhr.path.toString());
            Assert.assertEquals("HTTP/1.1", hhr.protocol);
            Assert.assertEquals("foo=bar", hhr.queryString);
            Assert.assertEquals(true, hhr.complete);

            Assert.assertEquals("example.com", hhr.headers.get("Host").getValue());
            Assert.assertEquals("keep-alive", hhr.headers.get("Connection").getValue());
            Assert.assertEquals("no-cache", hhr.headers.get("Cache-Control").getValue());
            Assert.assertEquals("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8", hhr.headers.get("Accept").getValue());
            Assert.assertEquals("no-cache", hhr.headers.get("Pragma").getValue());
            Assert.assertEquals("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.114 Safari/537.36", hhr.headers.get("User-Agent").getValue());
            Assert.assertEquals("gzip,deflate,sdch", hhr.headers.get("Accept-Encoding").getValue());
            Assert.assertEquals("en-US,en;q=0.8,ru;q=0.6", hhr.headers.get("Accept-Language").getValue());
            Assert.assertEquals("JSESSIONID=1dt8eiw5zc9t4j2o9asxcgmzq; __utma=107222046.2138525965.1372169768.1372169768.1372685422.2; __utmz=107222046.1372169768.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)", hhr.headers.get("Cookie").getValue());
        };
        complexTest(src, checker);


        src = "GET /http/? HTTP/1.1\r\n" +
                "Host: example.com\r\n" +
                "\r\n";
        checker = hhr -> {
            Assert.assertEquals("GET", hhr.method);
            Assert.assertEquals("/http/", hhr.path.toString());
            Assert.assertEquals("HTTP/1.1", hhr.protocol);
            Assert.assertEquals("", hhr.queryString);
            Assert.assertEquals(true, hhr.complete);

            Assert.assertEquals("example.com", hhr.headers.get("Host").getValue());
        };
        complexTest(src, checker);


        src = "GET /http/ HTTP/1.1\r\n" +
                "Host: example.com\r\n" +
                "\r\n";
        checker = hhr -> {
            Assert.assertEquals("GET", hhr.method);
            Assert.assertEquals("/http/", hhr.path.toString());
            Assert.assertEquals("HTTP/1.1", hhr.protocol);
            Assert.assertEquals(null, hhr.queryString);
            Assert.assertEquals(true, hhr.complete);

            Assert.assertEquals("example.com", hhr.headers.get("Host").getValue());
        };
        complexTest(src, checker);
    }

    public void complexTest(String src, Checker checker) {
        test1(src, checker);
        test2(src, checker);
        test3(src, checker);
        test4(src, checker);
        test6(src, checker);
        testAll(src, checker);
    }

    public void test1(String src, Checker checker) {
        RequestReader reader = new RequestReader();
        Assert.assertEquals(src.indexOf("\r\n\r\n") + 4, reader.read(src.getBytes()));
        checker.check(reader);
    }

    public void test2(String src, Checker checker) {
        RequestReader reader = new RequestReader();
        byte[] bytes = src.getBytes();
        for (int i = 0; i < bytes.length && !reader.complete; i++) {
            reader.read(bytes, i, 1);
        }

        checker.check(reader);
    }

    public void test3(String src, Checker checker) {
        RequestReader reader = new RequestReader();
        byte[] bytes = src.getBytes();
        for (int i = 0; i < bytes.length && !reader.complete; i += 2) {
            reader.read(bytes, i, 2);
        }

        checker.check(reader);
    }

    public void test4(String src, Checker checker) {
        RequestReader reader = new RequestReader();
        byte[] bytes = src.getBytes();
        for (int i = 0; i < bytes.length && !reader.complete; i += 3) {
            reader.read(bytes, i, 3);
        }

        checker.check(reader);
    }

    public void test6(String src, Checker checker) {
        RequestReader reader = new RequestReader();
        byte[] bytes = src.getBytes();
        int j = 7;
        for (int i = 0; i < bytes.length && !reader.complete; i += j) {
            reader.read(bytes, i, i + j > bytes.length ? bytes.length - i : j);
        }

        checker.check(reader);
    }

    public void testAll(String src, Checker checker) {
        RequestReader reader;
        byte[] bytes = src.getBytes();
        for (int j = 1; j < bytes.length; j++) {
            reader = new RequestReader();
//            System.out.println(j);
            for (int i = 0; i < bytes.length && !reader.complete; i += j) {
                reader.read(bytes, i, i + j > bytes.length ? bytes.length - i : j);
            }
            checker.check(reader);
        }
    }

    //    @Test
    public void benchmark() throws InterruptedException {
        File dir = new File("/tmp/httpBenchmark");

        byte[][] data = new byte[100][];
        for (int i = 0; i < 100; i++) {
            data[i] = FileTools.bytes(new File(dir, "" + i));
        }
        int n = 100000;

        for (int i = 0; i < 10; i++) {
            long time = System.currentTimeMillis();
            long totalBytes = 0;
            byte[] bytes;


            for (int j = 0; j < n; j++) {
                RequestReader reader = new RequestReader();
                bytes = data[j % 100];
                totalBytes += bytes.length;
                reader.read(bytes);
                assert reader.complete;
            }

            time = System.currentTimeMillis() - time;
            System.out.println("time: " + time + "\t\t" + (totalBytes * 1000d / time / 1024 / 1024));
            Thread.sleep(2000);
        }


        System.out.println();
        System.out.println();
        System.out.println("stupid method");
        for (int i = 0; i < 10; i++) {
            long time = System.currentTimeMillis();
            long totalBytes = 0;
            byte[] bytes;
            for (int j = 0; j < n; j++) {
                bytes = data[j % 100];
                totalBytes += bytes.length;
                stupid(bytes);
            }
            time = System.currentTimeMillis() - time;
            System.out.println("time: " + time + "\t\t" + (totalBytes * 1000d / time / 1024 / 1024));
            Thread.sleep(2000);
        }
    }

    private int stupid(byte[] data) {
        int r = data.length;
        int position;
        int from = 0;
        byte b;
        for (int i = from; i < r; i += 4) {
            b = data[i];
            if (b == 13) {
                if (data[i + 2] == 13) {
                    position = i + 4;
                    return position;
                }
                if (data[i - 2] == 13) {
                    position = i + 2;
                    return position;
                }
            } else if (b == 10) {
                if (data[i + 2] == 10) {
                    position = i + 3;
                    return position;
                }
                if (data[i - 2] == 10) {
                    position = i + 1;
                    return position;
                }
            }
        }
        return -1;
    }

    @Test
    public void testWhiteSpaces() {
        String data = "GET    /http/    HTTP/1.1 \r\n" +
                "Host:   example.com\r\n\r\ndata";

        RequestReader reader = new RequestReader();
        reader.read(data.getBytes());

        Assert.assertEquals(true, reader.complete);
        Assert.assertEquals("GET", reader.method);
        Assert.assertEquals("/http/", reader.path.toString());
        Assert.assertEquals("HTTP/1.1", reader.protocol);

        Assert.assertEquals("example.com", reader.headers.get("Host").getValue());
    }

    //    @Test
    public void testServers() throws IOException {
//        byte[] bytes = FileTools.bytes("/tmp/httpBenchmark/0");
        File dir = new File("/tmp/httpBenchmark");

        byte[][] data = new byte[100][];
        for (int i = 0; i < 100; i++) {
            data[i] = FileTools.bytes(new File(dir, "" + i));
        }

        byte[] response = new byte[1024];
        for (int i = 0; i < 10; i++) {
            long time = System.currentTimeMillis();
            long totalBytes = 0;
            int r = 0;
            byte[] bytes;
            Socket s = null;
            s = new Socket("localhost", 8083);
            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();
            for (int j = 0; j < 100000; j++) {
                bytes = data[j % 100];
                totalBytes += bytes.length;
//                System.out.println(new String(bytes));
                try {
                    out.write(bytes);
                    out.flush();
                    r = in.read(response);
//                System.out.println(new String(response, 0, r));
//                System.out.println(new String(bytes,0,r));
                } catch (Exception e) {
                    s = new Socket("localhost", 8080);
                    out = s.getOutputStream();
                    in = s.getInputStream();
                }
            }
            s.close();
            time = System.currentTimeMillis() - time;
            System.out.println("time: " + time + "\t\t" + (totalBytes * 1000d / time / 1024 / 1024));
        }
    }

    //    @Test
    public void prepareBenchmark() {
        File dir = new File("/tmp/httpBenchmark");
        dir.mkdirs();
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("GET /1 HTTP/1.1\r\n");
            sb.append("Connection: Keep-Alive\r\n");
            sb.append("Host: localhost\r\n");
            for (int j = 0; j < 50; j++) {
                sb.append(MD5.getMD5AsString(String.valueOf(random.nextInt())));
                sb.append(": ");
                sb.append(MD5.getMD5AsString(String.valueOf(random.nextInt())));
                sb.append("\r\n");
            }
            sb.append("\r\n");
            FileTools.text(new File(dir, "" + i), sb.toString());
        }
    }
}