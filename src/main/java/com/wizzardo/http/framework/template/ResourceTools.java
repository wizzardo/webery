package com.wizzardo.http.framework.template;

import com.wizzardo.tools.collections.flow.Filter;
import com.wizzardo.tools.misc.Consumer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author: moxa
 * Date: 5/15/13
 */
public interface ResourceTools {
    InputStream getResource(String path) throws IOException;

    File getResourceFile(String path);

    void getResourceFile(String path, Consumer<File> consumer);

    String getResourceAsString(String path);

    List<Class> getClasses();

    ResourceTools addClasspathFilter(Filter<String> filter);

    List<Filter<String>> getClasspathFilters();

    void addPathToClasses(String path);

    void addResourcesDir(File dir);
}
