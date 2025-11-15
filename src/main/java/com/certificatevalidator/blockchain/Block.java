package com.certificatevalidator.blockchain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single block in the blockchain.
 * Each block contains certificate information and maintains chain integrity.
 */
public class Block {
    private final int index;
    private final String timestamp;
    private final String certificateHash;
    private final String previousHash;
    private final String currentHash;

    /**
     * Constructor for creating a new block
     * @param index Block index in the chain
     * @param certificateHash Hash of the certificate data
     * @param previousHash Hash of the previous block
     */
    public Block(int index, String certificateHash, String previousHash) {
        this.index = index;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.certificateHash = certificateHash;
        this.previousHash = previousHash;
        this.currentHash = calculateHash();
    }

    /**
     * Constructor for loading existing block from database
     * @param index Block index
     * @param timestamp Block timestamp
     * @param certificateHash Certificate hash
     * @param previousHash Previous block hash
     * @param currentHash Current block hash
     */
    public Block(int index, String timestamp, String certificateHash, String previousHash, String currentHash) {
        this.index = index;
        this.timestamp = timestamp;
        this.certificateHash = certificateHash;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
    }

    /**
     * Calculates the hash of the current block using SHA-256
     * @return SHA-256 hash of the block data
     */
    private String calculateHash() {
        String data = index + timestamp + certificateHash + previousHash;
        return applySHA256(data);
    }

    /**
     * Applies SHA-256 hashing to the input string
     * @param input String to hash
     * @return SHA-256 hash
     */
    private String applySHA256(String input) {
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
            throw new RuntimeException("Error calculating hash", e);
        }
    }

    // Getters
    public int getIndex() {
        return index;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getCertificateHash() {
        return certificateHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    /**
     * Validates the block's hash integrity
     * @return true if the block's hash is valid
     */
    public boolean isValid() {
        String calculatedHash = calculateHash();
        return calculatedHash.equals(currentHash);
    }

    @Override
    public String toString() {
        return String.format("Block{index=%d, timestamp='%s', certificateHash='%s', previousHash='%s', currentHash='%s'}", 
                index, timestamp, certificateHash, previousHash, currentHash);
    }
}
