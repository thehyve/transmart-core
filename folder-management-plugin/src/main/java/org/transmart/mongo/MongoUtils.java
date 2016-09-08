package org.transmart.mongo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.net.util.Base64;


public class MongoUtils {
	public static final String ALGORITHM = "SHA-256";
	public static String hash(String clear) throws NoSuchAlgorithmException {
	                MessageDigest digest;
	                digest = MessageDigest.getInstance(ALGORITHM);
	                byte[] bin = digest.digest(clear.getBytes());
	                return Base64.encodeBase64String(bin).replace("\n", "");
	        }
}
