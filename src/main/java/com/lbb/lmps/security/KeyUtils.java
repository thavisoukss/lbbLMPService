package com.lbb.lmps.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyUtils {

    public static PrivateKey loadPrivateKey() throws Exception {
        String keyContent = getPrivateKeyContent();
        String cleanKey = keyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(cleanKey);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    public static PublicKey loadPublicKey() throws Exception {
        String keyContent = getPublicKeyContent();
        String cleanKey = keyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(cleanKey);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private static String getPrivateKeyContent() throws Exception {
        // 1. Try direct environment variable
        String envKey = System.getenv("JWT_PRIVATE_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey;
        }

        // 2. Try file path from environment variable or system property
        String keyPath = System.getenv("JWT_PRIVATE_KEY_PATH");
        if (keyPath == null || keyPath.trim().isEmpty()) {
            keyPath = System.getProperty("jwt.private.key.path");
        }

        if (keyPath != null && !keyPath.trim().isEmpty()) {
            File file = new File(keyPath);
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        // 3. Fallback to classpath resource based on active profile
        String profile = getActiveProfile();
        if ("prod".equals(profile) || "pro".equals(profile) || "production".equals(profile)) {
            throw new SecurityException("Production private key must be injected externally (using environment variables JWT_PRIVATE_KEY or JWT_PRIVATE_KEY_PATH). Committing production private keys to source code is prohibited.");
        }

        String resourceName = "keys/uat-private.pem";
        try (InputStream is = KeyUtils.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new RuntimeException(resourceName + " not found in classpath or external path");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String getPublicKeyContent() throws Exception {
        // 1. Try direct environment variable
        String envKey = System.getenv("JWT_PUBLIC_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey;
        }

        // 2. Try file path from environment variable or system property
        String keyPath = System.getenv("JWT_PUBLIC_KEY_PATH");
        if (keyPath == null || keyPath.trim().isEmpty()) {
            keyPath = System.getProperty("jwt.public.key.path");
        }

        if (keyPath != null && !keyPath.trim().isEmpty()) {
            File file = new File(keyPath);
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        // 3. Fallback to classpath resource based on active profile
        String profile = getActiveProfile();
        String resourceName = ("prod".equals(profile) || "pro".equals(profile) || "production".equals(profile))
                ? "keys/prod-public.pem"
                : "keys/uat-public.pem";

        try (InputStream is = KeyUtils.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new RuntimeException(resourceName + " not found in classpath or external path");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String getActiveProfile() {
        // 1. Try system property
        String profile = System.getProperty("spring.profiles.active");
        if (profile != null && !profile.trim().isEmpty()) {
            return profile.trim().toLowerCase();
        }

        // 2. Try env var
        profile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (profile != null && !profile.trim().isEmpty()) {
            return profile.trim().toLowerCase();
        }

        // 3. Parse application.yaml as plain text to find active profile
        try (InputStream is = KeyUtils.class.getClassLoader().getResourceAsStream("application.yaml")) {
            if (is != null) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                for (String line : content.split("\n")) {
                    line = line.trim();
                    if (line.startsWith("active:") || line.replaceAll("\\s+", "").startsWith("active:")) {
                        String[] parts = line.split(":", 2);
                        if (parts.length > 1) {
                            String active = parts[1].trim().replaceAll("[#\"']", "");
                            if (active.contains(" ")) {
                                active = active.split("\\s+")[0];
                            }
                            if (!active.isEmpty()) {
                                return active.toLowerCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore and fallback
        }

        return "uat"; // Fallback default
    }
}