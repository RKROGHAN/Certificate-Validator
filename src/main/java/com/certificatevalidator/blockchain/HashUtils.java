package com.certificatevalidator.blockchain;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for SHA-256 hashing operations used throughout the blockchain.
 */
public class HashUtils {
    
    /**
     * Applies SHA-256 hashing to the input string
     * @param input String to hash
     * @return SHA-256 hash as hexadecimal string
     */
    public static String applySHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Error applying SHA-256 hash", e);
        }
    }

    /**
     * Generates a hash for certificate data
     * @param studentName Student name
     * @param course Course name
     * @param issueDate Issue date as string
     * @return SHA-256 hash of the certificate data
     */
    public static String generateCertificateHash(String studentName, String course, String issueDate) {
        String data = studentName + course + issueDate;
        return applySHA256(data);
    }

    /**
     * Validates if a given hash matches the expected hash for certificate data
     * @param studentName Student name
     * @param course Course name
     * @param issueDate Issue date as string
     * @param expectedHash The hash to validate against
     * @return true if the hash is valid
     */
    public static boolean validateCertificateHash(String studentName, String course, String issueDate, String expectedHash) {
        String calculatedHash = generateCertificateHash(studentName, course, issueDate);
        return calculatedHash.equals(expectedHash);
    }

    /**
     * Generates SHA-256 hash from file content
     * @param filePath Path to the file
     * @return SHA-256 hash of the file content
     */
    public static String hashFile(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            return hashInputStream(fis);
        }
    }

    /**
     * Generates SHA-256 hash from input stream
     * @param inputStream Input stream to hash
     * @return SHA-256 hash of the stream content
     */
    public static String hashInputStream(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing file", e);
        }
    }

    /**
     * Generates SHA-256 hash from byte array
     * @param data Byte array to hash
     * @return SHA-256 hash of the data
     */
    public static String hashBytes(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing bytes", e);
        }
    }
}
