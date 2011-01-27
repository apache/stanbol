package org.apache.stanbol.enhancer.engines.metaxa.core.html;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

/**
 * EncodingDetector.java
 *
 * @author <a href="mailto:kasper@dfki.de">Walter Kasper</a>
 *
 */
public class CharsetRecognizer {

    /**
     * This contains the logger.
     */
    private static final Logger LOG =
        LoggerFactory.getLogger(CharsetRecognizer.class);


    private static String checkPattern(String str, String pattern, int group) {

        Pattern pat = Pattern.compile(pattern);
        Matcher m = pat.matcher(str);
        if (m.find()) {
            return m.group(group);
        }
        return null;
    }


    private static String checkFormat(String format, InputStream in)
            throws IOException {

        String result = null;
        String defaultValue = null;
        byte[] bytes;
        String decl;
        in.mark(4096);
        int read;
        if (format.equalsIgnoreCase("xml")) {
            defaultValue = "UTF-8";
            bytes = new byte[80];
            read = in.read(bytes);
            in.reset();
            decl = new String(bytes, 0, read, "US-ASCII");
            result = checkPattern(decl, "encoding=\"(\\w[-\\w]+)\"", 1);
        }
        else if (format.equalsIgnoreCase("html")) {
            bytes = new byte[2048];
            read = in.read(bytes);
            in.reset();
            decl = new String(bytes, 0, read, "US-ASCII");
            result =
                checkPattern(decl,
                "<meta .*?content=\".*charset=(\\w[-\\w]+).*?/>", 1);
        }
        if (result != null) {
            result = result.toUpperCase();
            LOG.info(format.toUpperCase() + " encoding: " + result);
        }
        else {
            return defaultValue;
        }
        return result;
    }


    public static String detect(InputStream in)
            throws IOException {

        return detect(in, null, null);
    }


    public static String detect(InputStream in, String format, String encoding)
            throws IOException {

        // the input stream must support marks
        if (!in.markSupported()) {
            throw new IOException("Mark not supported by input stream");
        }
        String result = null;
        if (format != null) {
            result = checkFormat(format, in);
            if (result != null) {
                return result;
            }
        }
        // in case of HTML or XML check whether there is a charset
        // specification; might be too fragile
        CharsetDetector detector = new CharsetDetector();
        if (encoding != null) {
            detector.setDeclaredEncoding(encoding);
        }
        detector.setText(in);
        CharsetMatch found = detector.detect();
        result = found.getName();
        LOG.info("Encoding: " + result);
        return result;
    }

    public static void main(String[] args) {

        String format = null;
        String encoding = null;
        int argv = 0;
        while (argv < args.length && args[argv].startsWith("-")) {
            String option = args[argv].substring(1);
            if (option.startsWith("f")) {
                format = args[++argv];
            }
            else if (option.startsWith("e")) {
                encoding = args[++argv];
            }
            else {
                System.err.println("illegal option: " + option);
                System.exit(1);
            }
            ++argv;
        }
        for (int i = argv; i < args.length; ++i) {
            try {
                BufferedInputStream fstream =
                    new BufferedInputStream(new FileInputStream(args[i]));
                String found =
                    CharsetRecognizer.detect(fstream, format, encoding);
                System.out.println("Encoding: " + found + ": " + args[i]);
                /*
                 * check whether the stream is reset correctly byte[] bytes =
                 * new byte[50]; int read = fstream.read(bytes);
                 * System.out.println(new String(bytes));
                 */
                fstream.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
    }

}
