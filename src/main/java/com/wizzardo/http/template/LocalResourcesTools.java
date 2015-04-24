package com.wizzardo.http.template;


import com.wizzardo.tools.io.ZipTools;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author: moxa
 * Date: 11/23/12
 */
public class LocalResourcesTools implements ResourceTools {

    private List<String> classpath = new ArrayList<String>();
    private List<File> resourcesDirs = new ArrayList<File>();

    {
        try {
            classpath.add(LocalResourcesTools.class.getProtectionDomain().getCodeSource().getLocation().toURI().getRawPath());
        } catch (Exception ignored) {
        }
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
        File f = path.startsWith("/") ? new File(path) : null;
        if (f == null || !f.exists()) {
            for (File dir : resourcesDirs) {
                f = new File(dir, path);
                if (f.exists())
                    return f;
            }
        }
        return null;
    }

    public String getResourceAsString(String path) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int r;
        byte[] b = new byte[10240];
        InputStream in = null;
        try {
            in = getResource(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (in == null) {
            return null;
        }

        try {
            while ((r = in.read(b)) != -1) {
                out.write(b, 0, r);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return out.toString("utf-8");
        } catch (UnsupportedEncodingException ignore) {
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
            for (File file : f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || (f.getName().endsWith(".class"));
                }
            })) {
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
        if (name.length() < 7)
            return null;
        try {
            name = name.substring(0, name.length() - 6).replace('/', '.');
            return ClassLoader.getSystemClassLoader().loadClass(name);
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }
}