package com.wizzardo.http.framework.template;


import com.wizzardo.http.framework.di.Injectable;
import com.wizzardo.tools.io.IOTools;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @author: moxa
 * Date: 11/23/12
 */
@Injectable
public class DevResourcesTools extends LocalResourcesTools {


    public InputStream getResource(String path) throws FileNotFoundException {
        File f = new File("src/main/resources/" + path);
        if (f.exists())
            return new FileInputStream(f);

        return super.getResource(path);
    }

    public File getResourceFile(String path) {
        File f = new File("src/main/resources/" + path);
        if (f.exists())
            return f;

        return super.getResourceFile(path);
    }

    public String getResourceAsString(String path) {
        try {
            return new String(IOTools.bytes(getResource(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
