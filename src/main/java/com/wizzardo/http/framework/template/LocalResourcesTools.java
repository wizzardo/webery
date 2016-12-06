package com.wizzardo.http.framework.template;


import com.wizzardo.http.framework.Environment;
import com.wizzardo.http.framework.Holders;
import com.wizzardo.http.framework.di.Injectable;
import com.wizzardo.tools.interfaces.Consumer;
import com.wizzardo.tools.interfaces.Filter;
import com.wizzardo.tools.io.FileTools;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.io.ZipTools;
import com.wizzardo.tools.misc.Unchecked;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author: moxa
 * Date: 11/23/12
 */
@Injectable
public class LocalResourcesTools implements ResourceTools {

    protected List<String> classpath = new ArrayList<>();
    protected List<File> resourcesDirs = new ArrayList<>();
    protected List<Filter<String>> classpathFilters = new ArrayList<>();
    protected File unzippedJar;

    {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader) cl).getURLs();
        for (URL url : urls) {
            classpath.add(url.getFile());
        }

        File src = new File("src");
        if (src.exists() && src.isDirectory()) {
            addResourcesDir(src.getAbsoluteFile().getParentFile());
        }

        File jarFile = new File(LocalResourcesTools.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        if (jarFile.isFile()) {

            File outDir = new File(Unchecked.call(() -> File.createTempFile("---", null)).getParentFile(), jarFile.getName() + "_unzipped");
            if (outDir.exists())
                FileTools.deleteRecursive(outDir);

            ZipTools.unzip(jarFile, outDir, entry -> entry.getName().startsWith("public"));
            addResourcesDir(outDir);
            unzippedJar = outDir;
        }
    }

    public boolean isJar() {
        return unzippedJar != null;
    }

    public File getUnzippedJarDirectory() {
        return unzippedJar;
    }

    public InputStream getResource(String path) throws FileNotFoundException {
        InputStream in = LocalResourcesTools.class.getResourceAsStream(path.startsWith("/") ? path : "/" + path);
        if (in != null) {
            return in;
        }
        File f = getResourceFile(path);
        if (f == null || !f.exists())
            throw new FileNotFoundException("file " + path + " not found");
        return new FileInputStream(f);
    }

    public void getResourceFile(String path, Consumer<File> consumer) {
        File file = getResourceFile(path);
        if (file != null && file.exists()) {
            consumer.consume(file);
        }
    }

    public File getResourceFile(String path) {
        File f;
        try {
            f = new File(LocalResourcesTools.class.getClassLoader().getResource(path).toURI());
            if (f.exists())
                return f;
        } catch (Exception ignored) {
        }

        f = path.startsWith("/") ? new File(path) : null;
        if (f != null && f.exists())
            return f;

        for (File dir : resourcesDirs) {
            f = new File(dir, path);
            if (f.exists())
                return f;
        }
        return null;
    }

    public String getResourceAsString(String path) {
        return Unchecked.ignore(() -> new String(IOTools.bytes(getResource(path)), StandardCharsets.UTF_8));
    }

    @Override
    public List<Class> getClasses() {
        List<Class> l = new ArrayList<Class>();
        File dir;

        System.out.println("classpath: " + classpath);

        for (String path : classpath) {
            dir = new File(path);
            if (!filterClasspath(dir))
                continue;

            if (Holders.getEnvironment() != Environment.TEST)
                System.out.println("searching for classes in " + dir.getAbsolutePath());

            if (!dir.exists())
                continue;
            if (dir.isDirectory())
                getClasses(dir, dir, l);
            else if (ZipTools.isZip(dir)) {
                getClasses(dir, l);
            }
        }
        return l;
    }

    @Override
    public ResourceTools addClasspathFilter(Filter<String> filter) {
        classpathFilters.add(filter);
        return this;
    }

    @Override
    public List<Filter<String>> getClasspathFilters() {
        return classpathFilters;
    }

    protected final static String[] classpathFiltersNotEndsWith = new String[]{
            "/jre/lib/charsets.jar",
            "/jre/lib/jfxswt.jar",
            "/jre/lib/resources.jar",
            "/jre/lib/jsse.jar",
            "/jre/lib/rt.jar",
            "/jre/lib/jce.jar",
            "/jre/lib/management-agent.jar",
            "/jre/lib/javaws.jar",
            "/jre/lib/plugin.jar",
            "/jre/lib/jfr.jar",
            "/jre/lib/deploy.jar",
            "/jre/lib/ext/sunjce_provider.jar",
            "/jre/lib/ext/sunec.jar",
            "/jre/lib/ext/localedata.jar",
            "/jre/lib/ext/jfxrt.jar",
            "/jre/lib/ext/dnsns.jar",
            "/jre/lib/ext/cldrdata.jar",
            "/jre/lib/ext/zipfs.jar",
            "/jre/lib/ext/nashorn.jar",
            "/jre/lib/ext/sunpkcs11.jar",
            "/jre/lib/ext/jaccess.jar",
            "/lib/idea_rt.jar",
            "/plugins/Groovy/lib/agent/gragent.jar",
    };

    protected boolean filterClasspath(File file) {
        String abs = file.getAbsolutePath();
        for (String s : classpathFiltersNotEndsWith) {
            if (abs.endsWith(s))
                return false;
        }

        return true;
    }

    @Override
    public void addPathToClasses(String path) {
        classpath.add(path);
    }

    @Override
    public void addResourcesDir(File dir) {
        if (dir.isFile())
            throw new IllegalArgumentException(dir.getAbsolutePath() + " - not a dir");

        System.out.println("addResourcesDir: " + dir.getAbsolutePath());
        resourcesDirs.add(dir);
    }


    protected void getClasses(File homeDir, File f, List<Class> l) {
        if (f.isDirectory()) {
            for (File file : f.listFiles(f1 -> f1.isDirectory() || (f1.getName().endsWith(".class")))) {
                getClasses(homeDir, file, l);
            }
        } else {
            String clazz = f.getAbsolutePath().substring(homeDir.getAbsolutePath().length() + 1);
            Class c = getClass(clazz);
            if (c != null)
                l.add(c);
        }
    }

    protected void getClasses(File archive, List<Class> l) {
        try {
            ZipInputStream zip = new ZipInputStream(new FileInputStream(archive));
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.toString();
                if (name.startsWith("WEB-INF/classes/"))
                    name = name.substring("WEB-INF/classes/".length());
                Class c = getClass(name);
                if (c != null)
                    l.add(c);
            }
            zip.close();
        } catch (IOException ignored) {
        }
    }

    protected Class getClass(String name) {
        if (name.length() < 7 || !name.endsWith(".class"))
            return null;
        try {
            name = name
                    .substring(0, name.length() - 6)
                    .replace(File.separatorChar, '.')
                    .replace('/', '.')
            ;
            if (!filterClass(name))
                return null;
            return ClassLoader.getSystemClassLoader().loadClass(name);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
        }
        return null;
    }

    protected boolean filterClass(String className) {
        for (Filter<String> filter : classpathFilters) {
            if (filter.allow(className))
                return true;
        }
        return false;
    }
}
