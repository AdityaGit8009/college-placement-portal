package edu.campus;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Security {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String token() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    private static byte[] derive(String password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    public static String hash(String password) throws Exception {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "210000$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derive(password, salt, 210000));
    }

    public static boolean verify(String password, String encoded) throws Exception {
        String[] p = encoded.split("\\$");
        return MessageDigest.isEqual(Base64.getDecoder().decode(p[2]),
                derive(password, Base64.getDecoder().decode(p[1]), Integer.parseInt(p[0])));
    }

    public static String html(Object value) {
        return String.valueOf(value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String config(String key, String fallback) {
        return System.getProperty(key, System.getenv().getOrDefault(key, fallback));
    }
}
