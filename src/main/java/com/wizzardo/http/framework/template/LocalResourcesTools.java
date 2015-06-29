package com.wizzardo.http.framework.template;


import com.wizzardo.http.framework.di.Injectable;
import com.wizzardo.tools.io.IOTools;
import com.wizzardo.tools.io.ZipTools;

import java.io.*;
import java.net.URISyntaxException;
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

    private List<String> classpath = new ArrayList<String>();
    private List<File> resourcesDirs = new ArrayList<File>();

    {
        ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader) cl).getURLs();
        for (URL url : urls) {
            classpath.add(url.getFile());
        }

//        try {
//            classpath.add(LocalResourcesTools.class.getProtectionDomain().getCodeSource().getLocation().toURI().getRawPath());
//        } catch (Exception ignored) {
//        }
        try {
            File workDir = new File(LocalResourcesTools.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (workDir.isFile())
                workDir = workDir.getParentFile();
            workDir = new File(workDir, "resources");
            if (workDir.exists())
                addResourcesDir(workDir);
        } catch (URISyntaxException ignore) {
        }
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
        try {
            return new String(IOTools.bytes(getResource(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    protected boolean filterClasspath(File file) {
        String abs = file.getAbsolutePath();
        if (abs.endsWith("/jre/lib/charsets.jar"))
            return false;
        if (abs.endsWith("/jre/lib/jfxswt.jar"))
            return false;
        if (abs.endsWith("/jre/lib/resources.jar"))
            return false;
        if (abs.endsWith("/jre/lib/jsse.jar"))
            return false;
        if (abs.endsWith("/jre/lib/rt.jar"))
            return false;
        if (abs.endsWith("/jre/lib/jce.jar"))
            return false;
        if (abs.endsWith("/jre/lib/management-agent.jar"))
            return false;
        if (abs.endsWith("/jre/lib/javaws.jar"))
            return false;
        if (abs.endsWith("/jre/lib/plugin.jar"))
            return false;
        if (abs.endsWith("/jre/lib/jfr.jar"))
            return false;
        if (abs.endsWith("/jre/lib/deploy.jar"))
            return false;
        if (abs.endsWith("/jre/lib/ext/sunjce_provider.jar"))
            return false;
        if (abs.endsWith("/jre/lib/ext/sunec.jar"))
            return false;
        if (abs.endsWith("/jre/lib/ext/localedata.jar"))
            return false;
        if (abs.endsWith("/jre/lib/ext/jfxrt.jar"))
            return false;
        if (abs.endsWith("/jre/lib/ext/dnsns.jar"))
            return false;
        if (abs.endsWith("/jre/lib/ext/cldrdata.jar"))
            return false;
        if (abs.endsWith("/jre/lib/ext/zipfs.jar"))
            return false;
        if (abs.endsWith("/jre/lib/ext/nashorn.jar"))
            return false;
        if (abs.endsWith("/jre/lib/ext/sunpkcs11.jar"))
            return false;
        if (abs.endsWith("/lib/idea_rt.jar"))
            return false;
        if (abs.endsWith("/plugins/Groovy/lib/agent/gragent.jar"))
            return false;

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


    private void getClasses(File homeDir, File f, List<Class> l) {
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

    private void getClasses(File archive, List<Class> l) {
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

    private Class getClass(String name) {
        if (name.length() < 7 || !name.endsWith(".class"))
            return null;
        try {
            name = name.substring(0, name.length() - 6).replace('/', '.');
            return ClassLoader.getSystemClassLoader().loadClass(name);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
        }
        return null;
    }
}
