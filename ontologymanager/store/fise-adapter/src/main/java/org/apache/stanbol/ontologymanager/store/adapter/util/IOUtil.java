package org.apache.stanbol.ontologymanager.store.adapter.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IOUtil {

    private IOUtil() {}

    private static class Holder {
        private static final IOUtil INSTANCE = new IOUtil();
    }

    public static IOUtil getInstance() {
        return Holder.INSTANCE;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtil.class.getName());

    public byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            LOGGER.warn("Content too long for file: " + file.getAbsolutePath());
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    public void writeBytesToFile(final File file, final byte[] bytes) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        for (byte b : bytes) {
            fos.write(b);
        }
        fos.flush();
        fos.close();
    }

    public String convertStreamToString(final InputStream is) throws IOException {
        if (is != null) {
            StringBuilder sb = new StringBuilder();
            String line;

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                is.close();
            }
            return sb.toString();
        } else {
            return "";
        }
    }
}
