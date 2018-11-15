package com.blocksim.utils;

import java.security.*;
import java.util.Base64;

/**
 * Utils file
 */
public class Utils {

    /**
     * Function referred from: http://www.baeldung.com/sha-256-hashing-java
     */
    public static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getStringFromKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static boolean verifyECDSASig(PublicKey pb, String data, byte[] signature) throws Exception {
        Signature ecdsa = Signature.getInstance("ECDSA", "BC");
        ecdsa.initVerify(pb);
        ecdsa.update(data.getBytes());
        return ecdsa.verify(signature);
    }

    public static byte[] applyECDSASig(PrivateKey pr, String data) throws Exception {
        Signature ecdsa = Signature.getInstance("ECDSA", "BC");
        ecdsa.initSign(pr);
        ecdsa.update(data.getBytes());
        return ecdsa.sign();
    }
}

