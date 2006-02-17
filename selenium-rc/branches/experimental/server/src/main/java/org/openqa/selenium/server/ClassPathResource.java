package org.openqa.selenium.server;

import org.mortbay.util.Resource;
import org.mortbay.util.IO;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;

/**
 * @author Patrick Lightbody (plightbo at gmail dot com)
 */
public class ClassPathResource extends Resource {
    String path;
    ByteArrayOutputStream os;

    public ClassPathResource(String path) {
        this.path = path;

        InputStream is = ClassPathResource.class.getResourceAsStream(path);
        if (is != null) {
            os = new ByteArrayOutputStream();
            try {
                IO.copy(is, os);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /* ------------------------------------------------------------ */
    public Object getAssociate() {
        return super.getAssociate();
    }

    public void release() {
    }

    public boolean exists() {
        return os != null;
    }

    public boolean isDirectory() {
        return false;
    }

    public long lastModified() {
        return 0;
    }

    public long length() {
        if (os != null) {
            return os.size();
        }

        return 0;
    }

    public URL getURL() {
        return null;
    }

    public File getFile() throws IOException {
        return null;
    }

    public String getName() {
        return null;
    }

    public InputStream getInputStream() throws IOException {
        if (os != null) {
            return new ByteArrayInputStream(os.toByteArray());
        }

        return null;
    }

    public OutputStream getOutputStream() throws IOException, SecurityException {
        return null;
    }

    public boolean delete() throws SecurityException {
        return false;
    }

    public boolean renameTo(Resource dest) throws SecurityException {
        return false;
    }

    public String[] list() {
        return new String[0];
    }

    public Resource addPath(String path) throws IOException, MalformedURLException {
        return new ClassPathResource(this.path + "/" + path);
    }
}
