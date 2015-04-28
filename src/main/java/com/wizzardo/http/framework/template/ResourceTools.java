package com.wizzardo.http.framework.template;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author: moxa
 * Date: 5/15/13
 */
public interface ResourceTools {
    public InputStream getResource(String path) throws IOException;

    public File getResourceFile(String path);

    public String getResourceAsString(String path);

    public List<Class> getClasses();

    public void addPathToClasses(String path);

    public void addResourcesDir(File dir);
}
