/*
 * FileUtil.java
 *
 * Created on November 15, 2007, 3:34 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */
package org.apache.stanbol.ontologymanager.store.rest.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author tuncay
 */
public class FileUtil {

    private static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static byte[] getBytesFromFile(String fileURI) throws IOException {
        File file = new File(fileURI);
        return getBytes(file);
    }

    private static String readWholeFile(URI uri) throws IOException {
        BufferedReader buf;
        StringBuffer rules = new StringBuffer();
        FileInputStream fis = new FileInputStream(new File(uri));
        InputStreamReader inputStreamReader = new InputStreamReader(fis, "UTF-8");
        buf = new BufferedReader(inputStreamReader);
        String temp;
        while ((temp = buf.readLine()) != null)
            rules.append(temp).append("\n");
        buf.close();
        return rules.toString();
    }

    public static String readWholeFile(String filePath) throws IOException {
        BufferedReader buf;
        StringBuffer rules = new StringBuffer();
        FileInputStream fis = new FileInputStream(filePath);
        InputStreamReader inputStreamReader = new InputStreamReader(fis, "UTF-8");
        buf = new BufferedReader(inputStreamReader);
        String temp;
        while ((temp = buf.readLine()) != null)
            rules.append(temp).append("\n");
        buf.close();
        return rules.toString();
    }

    public static String readWholeFile(InputStream is) throws IOException {
        BufferedReader buf;
        StringBuffer rules = new StringBuffer();
        InputStreamReader inputStreamReader = new InputStreamReader(is, "UTF-8");
        buf = new BufferedReader(inputStreamReader);
        String temp;
        while ((temp = buf.readLine()) != null)
            rules.append(temp).append("\n");
        buf.close();
        return rules.toString();
    }

    public static void appendToFile(String filePath, String content) throws IOException {
        PrintWriter outFile = new PrintWriter(filePath); // opens file
        outFile.print(content); // writes to file
        outFile.flush();
        outFile.close(); // closes the file
    }

    /*
     * public static String readWholeFile(String filePath, String encoding) throws IOException {
     * BufferedReader buf; StringBuffer rules=new StringBuffer(); FileInputStream fis = new
     * FileInputStream(filePath); InputStreamReader inputStreamReader = new InputStreamReader(fis, encoding);
     * buf=new BufferedReader(inputStreamReader); String temp; while((temp=buf.readLine())!=null)
     * rules.append(temp).append("\n"); buf.close(); return rules.toString(); }
     */

    /*
     * public static String readWebAppFile(String filePath, String encoding) throws IOException {
     * BufferedReader buf; StringBuffer rules=new StringBuffer(); InputStream is =
     * Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath); InputStreamReader
     * inputStreamReader = new InputStreamReader(is, encoding); buf=new BufferedReader(inputStreamReader);
     * String temp; while((temp=buf.readLine())!=null) rules.append(temp).append("\n"); buf.close(); return
     * rules.toString(); }
     */

    public static byte[] readFromURI(URI uri) throws IOException {
        if (uri.toString().contains("http:")) {
            URL url = uri.toURL();
            URLConnection urlConnection = url.openConnection();
            int length = urlConnection.getContentLength();
            logger.info("length of content in URL = " + length);
            if (length > -1) {
                byte[] pureContent = new byte[length];
                DataInputStream dis = new DataInputStream(urlConnection.getInputStream());
                dis.readFully(pureContent, 0, length);
                dis.close();

                return pureContent;
            } else {
                throw new IOException("Unable to determine the content-length of the document pointed at "
                                      + url.toString());
            }
        } else {
            return readWholeFile(uri).getBytes("UTF-8");
        }
    }

    private static byte[] getBytes(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        // logger.debug("\nFileInputStream is " + file);

        // Get the size of the file
        long length = file.length();
        // logger.debug("Length of " + file + " is " + length + "\n");
        if (length > Integer.MAX_VALUE) {
            logger.error("File is too large to process");
            return null;
        }
        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while ((offset < bytes.length) && ((numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)) {

            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    /**
     * Constructs a new file with the given content and filePath. If a file with the same name already exists,
     * simply overwrites the content.
     * 
     * @author Gunes
     * @param filePath
     * @param content
     *            : expressed as byte[]
     * @throws IOException
     *             : [approved by gunes] when the file cannot be created. possible causes: 1) invalid
     *             filePath, 2) already existing file cannot be deleted due to read-write locks
     */
    public static void constructNewFile(String filePath, byte[] content) throws IOException {
        File file = new File(filePath);
        file.mkdirs();
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(content);
        fos.close();
    }

    public static void writeToFile(File file, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        OutputStreamWriter outStreamWriter = new OutputStreamWriter(fos, "UTF-8");
        BufferedWriter bufferedWriter = new BufferedWriter(outStreamWriter);
        bufferedWriter.write(content);
        bufferedWriter.flush();
        bufferedWriter.close();
    }
}
