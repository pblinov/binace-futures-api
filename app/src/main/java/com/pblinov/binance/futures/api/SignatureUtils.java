package com.pblinov.binance.futures.api;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

class SignatureUtils {
    public static String sign(String query, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        var sha256 = Mac.getInstance("HmacSHA256");
        var secretKey = new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256");
        sha256.init(secretKey);
        return Hex.encodeHexString(sha256.doFinal(query.getBytes(UTF_8)));
    }
}
