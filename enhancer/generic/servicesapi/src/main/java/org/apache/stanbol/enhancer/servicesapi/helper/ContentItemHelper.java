package org.apache.stanbol.enhancer.servicesapi.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.clerezza.rdf.core.UriRef;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;


/**
 * Helper class to factorize common code for ContentItem handling.
 *
 * @author ogrisel
 */
public class ContentItemHelper {

    public static final String SHA1 = "SHA1";

    public static final int MIN_BUF_SIZE = 8 * 1024; // 8 kB

    public static final int MAX_BUF_SIZE = 64 * 1024; // 64 kB

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    // TODO: instead of using a static helper, build an OSGi component with a
    // configurable site-wide URI namespace for ids that are local to the
    // server.

    /**
     * Check that ContentItem#getId returns a valid URI or make an urn out of
     * it.
     */
    public static UriRef ensureUri(ContentItem ci) {
        String uri = ci.getId();
        if (!uri.startsWith("http://") && !uri.startsWith("urn:")) {
            uri = "urn:" + urlEncode(uri);
        }
        return new UriRef(uri);
    }

    public static String urlEncode(String uriPart) {
        try {
            return URLEncoder.encode(uriPart, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // will never happen since every unicode symbol can be encoded
            // to UTF-8
            return null;
        }
    }

    /**
     * Pass the binary content from in to out (if not null) while computing the
     * digest. Digest can typically be used to build ContentItem ids that map
     * the binary content of the array.
     *
     * @param in stream to read the data from
     * @param out optional output stream to
     * @param digestAlgorithm MD5 or SHA1 for instance
     * @return an hexadecimal representation of the digest
     * @throws IOException
     */
    public static String streamDigest(InputStream in, OutputStream out,
            String digestAlgorithm) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(digestAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            throw (IOException) new IOException().initCause(e);
        }

        int size = in.available();
        if (size == 0) {
            size = MAX_BUF_SIZE;
        } else if (size < MIN_BUF_SIZE) {
            size = MIN_BUF_SIZE;
        } else if (size > MAX_BUF_SIZE) {
            size = MAX_BUF_SIZE;
        }
        byte[] buf = new byte[size];

        /*
         * Copy and digest.
         */
        int n;
        while ((n = in.read(buf)) != -1) {
            if (out != null) {
                out.write(buf, 0, n);
            }
            digest.update(buf, 0, n);
        }
        if (out != null) {
            out.flush();
        }
        return toHexString(digest.digest());
    }

    public static String toHexString(byte[] data) {
        StringBuilder buf = new StringBuilder(2 * data.length);
        for (byte b : data) {
            buf.append(HEX_DIGITS[(0xF0 & b) >> 4]);
            buf.append(HEX_DIGITS[0x0F & b]);
        }
        return buf.toString();
    }

    public static UriRef makeDefaultUrn(byte[] content) {
        return makeDefaultUri("urn:content-item-", content);
    }

    public static UriRef makeDefaultUri(String baseUri, byte[] content) {
        // calculate an ID based on the digest of the content
        if (!baseUri.startsWith("urn:") && !baseUri.endsWith("/")) {
            baseUri += "/";
        }
        String hexDigest = "";
        try {
            hexDigest = streamDigest(new ByteArrayInputStream(content), null, SHA1);
        } catch (IOException e) {
            // this is not going to happen since output stream is null and the
            // input data is already loaded in memory
        }
        return new UriRef(baseUri + SHA1.toLowerCase() + "-" + hexDigest);
    }

}
