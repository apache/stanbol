/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.stanbol.rules.refactor.api.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class URIGenerator {

	
	public static final String SHA1 = "SHA1";

    public static final int MIN_BUF_SIZE = 8 * 1024; // 8 kB

    public static final int MAX_BUF_SIZE = 64 * 1024; // 64 kB
    
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
	
	
	public static String createID(String baseUri, byte[] content) {
	
		
        // calculate an ID based on the digest of the content
        String hexDigest = "";
        if (!baseUri.startsWith("urn:") && !baseUri.endsWith("/")) {
            baseUri = baseUri + "/";
        }
        try {
            hexDigest = streamDigest(
                    new ByteArrayInputStream(content), null, SHA1);
        } catch (IOException e) {
            // this is not going to happen since output stream is null and the
            // input data is already loaded in memory
        }
        
        return baseUri + SHA1.toLowerCase() + "-" + hexDigest;
    }
    
    
    
    
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
}
