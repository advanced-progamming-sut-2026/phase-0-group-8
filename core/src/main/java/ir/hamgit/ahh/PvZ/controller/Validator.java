package ir.hamgit.ahh.PvZ.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Validator {

    public static boolean isValidUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return username.matches("[a-zA-Z0-9\\-]+");
    }

    public static String isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter.";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Password must contain at least one digit.";
        }
        if (!password.matches(".*[!#$%^&*()=+}\\{\\[\\]|/\\\\:;'\",.><? ].*")) {
            return "Password must contain at least one special character.";
        }
        return null;
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        if (email.chars().filter(c -> c == '@').count() != 1) {
            return false;
        }
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false;
        }
        String local = parts[0];
        String domain = parts[1];
        if (!local.matches("[a-zA-Z0-9._\\-]+")) {
            return false;
        }
        if (local.length() > 1 && !local.matches("^[a-zA-Z0-9].*[a-zA-Z0-9]$")) {
            return false;
        }
        if (local.contains("..")) {
            return false;
        }
        if (!domain.contains(".")) {
            return false;
        }
        if (domain.startsWith(".") || domain.endsWith(".")) {
            return false;
        }
        if (domain.contains("..")) {
            return false;
        }
        String[] domainParts = domain.split("\\.");
        String tld = domainParts[domainParts.length - 1];
        if (tld.length() < 2) {
            return false;
        }
        for (String part : domainParts) {
            if (!part.matches("[a-zA-Z0-9\\-]+")) {
                return false;
            }
            if (part.startsWith("-") || part.endsWith("-")) {
                return false;
            }
        }
        if (email.matches(".*[!#$%^&*()=+}\\{\\[\\]|/\\\\:;'\"<>?].*")) {
            return false;
        }
        return true;
    }

    public static boolean isValidNickname(String nickname) {
        if (nickname == null) {
            return false;
        }
        return nickname.length() >= 3 && nickname.length() <= 30;
    }

    public static String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
