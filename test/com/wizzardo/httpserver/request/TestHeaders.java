package com.wizzardo.httpserver.request;

import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.security.MD5;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author: moxa
 * Date: 12/2/13
 */
public class TestHeaders {


    String src = "GET /http/ HTTP/1.1\r\n" +
            "Host: moxa.no-ip.biz\r\n" +
            "Connection: keep-alive\r\n" +
            "Cache-Control: no-cache\r\n" +
            "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n" +
            "Pragma: no-cache\r\n" +
            "User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.114 Safari/537.36\r\n" +
            "Accept-Encoding: gzip,deflate,sdch\r\n" +
            "Accept-Language: en-US,en;q=0.8,ru;q=0.6\r\n" +
            "Cookie: JSESSIONID=1dt8eiw5zc9t4j2o9asxcgmzq; __utma=107222046.2138525965.1372169768.1372169768.1372685422.2; __utmz=107222046.1372169768.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)\r\n" +
            "\r\nololo data foo bar";

    HttpHeadersReader hhr = new HttpHeadersReader();

    @Test
    public void test1() {
        Assert.assertEquals(src.indexOf("\r\n\r\n") + 4, hhr.read(src.getBytes()));
        test(hhr);
    }

    @Test
    public void test2() {
        byte[] bytes = src.getBytes();
        for (int i = 0; i < bytes.length && !hhr.complete; i++) {
            hhr.read(bytes, i, 1);
        }

        test(hhr);
    }

    @Test
    public void test3() {
        byte[] bytes = src.getBytes();
        for (int i = 0; i < bytes.length && !hhr.complete; i += 2) {
            hhr.read(bytes, i, 2);
        }

        test(hhr);
    }

    @Test
    public void test4() {
        byte[] bytes = src.getBytes();
        for (int i = 0; i < bytes.length && !hhr.complete; i += 3) {
            hhr.read(bytes, i, 3);
        }

        test(hhr);
    }
    @Test
    public void test6() {
        byte[] bytes = src.getBytes();
        int j = 7;
        for (int i = 0; i < bytes.length && !hhr.complete; i += j) {
            hhr.read(bytes, i, i+j>bytes.length?bytes.length-i:j);
        }

        test(hhr);
    }

    @Test
    public void test5() {
        byte[] bytes = src.getBytes();
        for (int j = 1; j < bytes.length; j++) {
            hhr = new HttpHeadersReader();
//            System.out.println(j);
            for (int i = 0; i < bytes.length && !hhr.complete; i += j) {
                hhr.read(bytes, i, i+j>bytes.length?bytes.length-i:j);
            }
            test(hhr);
        }
    }

    private void test(HttpHeadersReader hhr) {
        Assert.assertEquals("GET", hhr.method);
        Assert.assertEquals("/http/", hhr.path);
        Assert.assertEquals(true, hhr.complete);

        Assert.assertEquals("moxa.no-ip.biz", hhr.headers.get("Host"));
        Assert.assertEquals("keep-alive", hhr.headers.get("Connection"));
        Assert.assertEquals("no-cache", hhr.headers.get("Cache-Control"));
        Assert.assertEquals("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8", hhr.headers.get("Accept"));
        Assert.assertEquals("no-cache", hhr.headers.get("Pragma"));
        Assert.assertEquals("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.114 Safari/537.36", hhr.headers.get("User-Agent"));
        Assert.assertEquals("gzip,deflate,sdch", hhr.headers.get("Accept-Encoding"));
        Assert.assertEquals("en-US,en;q=0.8,ru;q=0.6", hhr.headers.get("Accept-Language"));
        Assert.assertEquals("JSESSIONID=1dt8eiw5zc9t4j2o9asxcgmzq; __utma=107222046.2138525965.1372169768.1372169768.1372685422.2; __utmz=107222046.1372169768.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)", hhr.headers.get("Cookie"));

    }

    //    @Test
    public void benchmarkHeaders() throws InterruptedException {
        Map<String, String> lhm = new LinkedHashMap<String, String>();
//        Map<String, String> headers = new Headers();
        RequestHeaders headers = new RequestHeaders();

        put("Host", "moxa.no-ip.biz", lhm, headers);
        put("Connection", "keep-alive", lhm, headers);
        put("Cache-Control", "no-cache", lhm, headers);
        put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8", lhm, headers);
        put("Pragma", "no-cache", lhm, headers);
        put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.114 Safari/537.36", lhm, headers);
        put("Accept-Encoding", "gzip,deflate,sdch", lhm, headers);
        put("Accept-Language", "en-US,en;q=0.8,ru;q=0.6", lhm, headers);
        put("Cookie", "JSESSIONID=1dt8eiw5zc9t4j2o9asxcgmzq; __utma=107222046.2138525965.1372169768.1372169768.1372685422.2; __utmz=107222046.1372169768.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)", lhm, headers);


        Assert.assertEquals("moxa.no-ip.biz", headers.get("Host"));
        Assert.assertEquals("keep-alive", headers.get("Connection"));
        Assert.assertEquals("no-cache", headers.get("Cache-Control"));
        Assert.assertEquals("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8", headers.get("Accept"));
        Assert.assertEquals("no-cache", headers.get("Pragma"));
        Assert.assertEquals("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.114 Safari/537.36", headers.get("User-Agent"));
        Assert.assertEquals("gzip,deflate,sdch", headers.get("Accept-Encoding"));
        Assert.assertEquals("en-US,en;q=0.8,ru;q=0.6", headers.get("Accept-Language"));
        Assert.assertEquals("JSESSIONID=1dt8eiw5zc9t4j2o9asxcgmzq; __utma=107222046.2138525965.1372169768.1372169768.1372685422.2; __utmz=107222046.1372169768.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none)", headers.get("Cookie"));


        int n = 10000000;
//        String key = new String("Host");
//        String key = new String("Cookie");
        String key = "Cookie";
//        String key = new String("User-Agent");
        long total = 0;

        for (int i = 0; i < 10; i++) {
            long time = System.currentTimeMillis();
            for (int j = 0; j < n; j++) {
                total += headers.get(key).length();
            }
            time = System.currentTimeMillis() - time;
            System.out.println("headers time:\t" + time);
            Thread.sleep(1000);


            time = System.currentTimeMillis();
            for (int j = 0; j < n; j++) {
                total += lhm.get(key).length();
            }
            time = System.currentTimeMillis() - time;
            System.out.println("lhm time:\t" + time);
            Thread.sleep(1000);

            System.out.println();
        }

        System.out.println("total: " + total);
    }

    private void put(String key, String value, Map<String, String> map1, Map<String, String> map2) {
        map1.put(key, value);
        map2.put(key, value);
    }

    private void put(String key, String value, Map<String, String> map1, RequestHeaders map2) {
        map1.put(key, value);
        map2.put(key, value);
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
                hhr = new HttpHeadersReader();
                bytes = data[j % 100];
                totalBytes += bytes.length;
                hhr.read(bytes);
                assert hhr.complete;
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