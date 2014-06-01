/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stanbol.commons.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility methods for converting passwords.
 *
 */
public final class PasswordUtil {

   /**
    * Restrict instantiation
    */
   private PasswordUtil() {}

   private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();
	
	/**
	 * @param bytes
	 *            array of bytes to be converted to a String of hexadecimal
	 *            numbers
	 * @return String of hexadecimal numbers representing the byte array
	 */
	public static String bytes2HexString(byte[] bytes) {
		char[] result = new char[bytes.length << 1];
		for (int i = 0, j = 0; i < bytes.length; i++) {
			result[j++] = HEXDIGITS[bytes[i] >> 4 & 0xF];
			result[j++] = HEXDIGITS[bytes[i] & 0xF];
		}
		return new String(result);
	}

	/**
	 * Encrypt the password with the SHA1 algorithm
	 *
	 * @param password
	 * @return the converted passwort as String
	 */
	public static String convertPassword(String password) {
		try {
			return bytes2HexString(MessageDigest.getInstance("SHA1").digest(
					password.getBytes("UTF-8")));
		} catch (NoSuchAlgorithmException nsae) {
			throw new RuntimeException();
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException();
		}
	}
}
