package com.wizzardo.http;

import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.misc.DateIso8601;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by wizzardo on 09/11/16.
 */
public class FileTreeHandlerTest extends ServerTest {
    File testDir;

    @Override
    public void setUp() throws NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        super.setUp();
        testDir = new File(FileTreeHandlerTest.class.getSimpleName());
        testDir.mkdirs();
    }

    @Override
    public void tearDown() throws InterruptedException {
        super.tearDown();
        FileTools.deleteRecursive(testDir);
    }

    @Test
    public void test_prefix() {
        FileTreeHandler handler;

        handler = new FileTreeHandler("/", "");
        Assert.assertEquals("", handler.prefix);

        handler = new FileTreeHandler("/", "/");
        Assert.assertEquals("", handler.prefix);

        handler = new FileTreeHandler("/", "prefix");
        Assert.assertEquals("/prefix", handler.prefix);

        handler = new FileTreeHandler("/", "prefix/");
        Assert.assertEquals("/prefix", handler.prefix);

        handler = new FileTreeHandler("/", "/prefix/");
        Assert.assertEquals("/prefix", handler.prefix);

        handler = new FileTreeHandler("/", "/prefix");
        Assert.assertEquals("/prefix", handler.prefix);
    }

    @Test
    public void test_formatFileSize() {
        FileTreeHandler handler = new FileTreeHandler("/", "");
        Assert.assertEquals("1", handler.formatFileSize(1));
        Assert.assertEquals("1K", handler.formatFileSize(1024));
        Assert.assertEquals("1M", handler.formatFileSize(1024 * 1024));
        Assert.assertEquals("1G", handler.formatFileSize(1024 * 1024 * 1024));
    }

    @Test
    public void test_encode() {
        FileTreeHandler handler = new FileTreeHandler("/", "");
        Assert.assertEquals("foo", handler.encodeName("foo"));
        Assert.assertEquals("foo%20bar", handler.encodeName("foo bar"));
        Assert.assertEquals("%D1%82%D0%B5%D1%81%D1%82", handler.encodeName("тест"));
    }

    @Test
    public void test_generateUrl() {
        FileTreeHandler handler = new FileTreeHandler("/", "");
        Assert.assertEquals("FileTreeHandlerTest/", handler.generateUrl(testDir, null));
        Assert.assertEquals("foo", handler.generateUrl(new File(testDir, "foo"), null));
    }

    @Test
    public void test_sort() throws InterruptedException {
        FileTreeHandler<FileTreeHandler.HandlerContext> handler = new FileTreeHandler<>("/", "");
        File dir1 = new File(testDir, "dir1");
        dir1.mkdirs();
        File dir2 = new File(testDir, "dir2");
        dir2.mkdirs();
        File file2 = new File(testDir, " file2");
        FileTools.text(file2, "foo");
        Thread.sleep(1001);
        File file1 = new File(testDir, " file1");
        FileTools.text(file1, "foobar");

        Comparator<File> comparator;
        comparator = handler.createFileComparator(1, false, false);

        Assert.assertEquals(-1, comparator.compare(dir1, file1));
        Assert.assertEquals(1, comparator.compare(file1, dir1));
        Assert.assertEquals(-1, comparator.compare(dir1, dir2));
        Assert.assertEquals(1, comparator.compare(dir2, dir1));

        comparator = handler.createFileComparator(1, true, false);
        Assert.assertEquals(1, comparator.compare(file1, file2));
        Assert.assertEquals(-1, comparator.compare(file2, file1));
        Assert.assertEquals(0, comparator.compare(file1, file1));

        comparator = handler.createFileComparator(1, false, true);
        Assert.assertEquals(1, comparator.compare(file1, file2));
        Assert.assertEquals(-1, comparator.compare(file2, file1));
        Assert.assertEquals(0, comparator.compare(file1, file1));
    }

    @Test
    public void test_handle() throws IOException, InterruptedException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException {
        FileTools.text(new File(testDir, "foo"), "bar");

        handler = new FileTreeHandler(testDir, "");
        Assert.assertEquals("bar", makeRequest("/foo").get().asString());

        handler = new FileTreeHandler(testDir, "prefix");
        Assert.assertEquals("bar", makeRequest("/prefix/foo").get().asString());
        checkResponse(400, "path must starts with prefix '/prefix'. path=/wrong_prefix/foo", makeRequest("/wrong_prefix/foo").get());
        checkResponse(400, "path must starts with prefix '/prefix'. path=/foo", makeRequest("/prefix/../foo").get());
        checkResponse(404, "/prefix/bar not found", makeRequest("/prefix/bar").get());

        Assert.assertTrue(new File(testDir, "foo").setReadable(false));
        if (!new File(testDir, "foo").canRead())
            checkResponse(403, "/prefix/foo is forbidden", makeRequest("/prefix/foo").get());

        new File(testDir, "bar").mkdirs();
        handler = new FileTreeHandler(testDir, "").setShowFolder(false);
        checkResponse(403, "/bar is forbidden", makeRequest("/bar").get());

        handler = new FileTreeHandler(testDir, "");
        checkResponse(200, "<!DOCTYPE html><html>" +
                "<header><meta charset=\"utf-8\"><title>/bar/</title></meta></header>" +
                "<body>" +
                "<h1><a href=\"/\">root: </a><a href=\"/\"></a>/<a href=\"/bar/\">bar</a>/</h1>" +
                "<table border=\"0\">" +
                "<tr>" +
                "<th><a href=\"/bar/?sort=name&order=desc\">Name</a></th>" +
                "<th><a href=\"/bar/?sort=modified&order=desc\">Last modified</a></th>" +
                "<th><a href=\"/bar/?sort=size&order=desc\">Size</a></th>" +
                "</tr>\n" +
                "</table></body></html>", makeRequest("/bar").get());


        tearDown();
        context = "context";
        setUp();
        handler = new FileTreeHandler(testDir, "");
        FileTools.text(new File(testDir, "foo"), "bar");
        Assert.assertEquals("bar", makeRequest("/" + context + "/foo").get().asString());
    }

    @Test
    public void test_handle_dir() throws IOException, InterruptedException, NoSuchMethodException, NoSuchFieldException, ClassNotFoundException {
        File subDir = new File(testDir, "sub");
        subDir.mkdirs();

        FileTools.text(new File(subDir, "foo"), "foo");
        FileTools.text(new File(subDir, "bar"), "bar");

        handler = new FileTreeHandler(testDir, "");
        Assert.assertEquals("<!DOCTYPE html><html><header><meta charset=\"utf-8\">" +
                "<title>/sub/</title>" +
                "</meta></header>" +
                "<body>" +
                "<h1><a href=\"/\">root: </a><a href=\"/\"></a>/<a href=\"/sub/\">sub</a>/</h1>" +
                "<table border=\"0\">" +
                "<tr><th><a href=\"/sub/?sort=name&order=desc\">Name</a></th><th><a href=\"/sub/?sort=modified&order=desc\">Last modified</a></th><th><a href=\"/sub/?sort=size&order=desc\">Size</a></th></tr>\n" +
                "<tr><td><a href=\"bar\">bar</a></td><td>" + DateIso8601.format(new Date(new File(subDir, "bar").lastModified())) + "</td><td align=\"right\">3</td></tr>\n" +
                "<tr><td><a href=\"foo\">foo</a></td><td>" + DateIso8601.format(new Date(new File(subDir, "foo").lastModified())) + "</td><td align=\"right\">3</td></tr>\n" +
                "</table></body></html>", makeRequest("/sub").get().asString());
    }
}
