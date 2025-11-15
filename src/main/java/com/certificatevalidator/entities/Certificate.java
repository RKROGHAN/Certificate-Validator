package com.certificatevalidator.entities;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Represents a certificate with all necessary information and hash generation.
 */
public class Certificate {
    private int id;
    private String studentName;
    private String course;
    private LocalDate issueDate;
    private String hash;

    /**
     * Constructor for creating a new certificate
     * @param studentName Name of the student
     * @param course Course name
     * @param issueDate Date when certificate was issued
     */
    public Certificate(String studentName, String course, LocalDate issueDate) {
        this.studentName = studentName;
        this.course = course;
        this.issueDate = issueDate;
        this.hash = generateHash();
    }

    /**
     * Constructor for loading existing certificate from database
     * @param id Certificate ID
     * @param studentName Student name
     * @param course Course name
     * @param issueDate Issue date
     * @param hash Certificate hash
     */
    public Certificate(int id, String studentName, String course, LocalDate issueDate, String hash) {
        this.id = id;
        this.studentName = studentName;
        this.course = course;
        this.issueDate = issueDate;
        this.hash = hash;
    }

    /**
     * Generates a SHA-256 hash for the certificate data
     * @return SHA-256 hash of the certificate
     */
    private String generateHash() {
        String data = studentName + course + issueDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
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
            throw new RuntimeException("Error generating certificate hash", e);
        }
    }

    /**
     * Validates the certificate's hash integrity
     * @return true if the certificate hash is valid
     */
    public boolean isValid() {
        String expectedHash = generateHash();
        return expectedHash.equals(hash);
    }

    /**
     * Gets certificate data as a formatted string for display
     * @return Formatted certificate information
     */
    public String getCertificateInfo() {
        return String.format("Certificate ID: %d\nStudent: %s\nCourse: %s\nIssue Date: %s\nHash: %s",
                id, studentName, course, issueDate.format(DateTimeFormatter.ISO_LOCAL_DATE), hash);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return String.format("Certificate{id=%d, studentName='%s', course='%s', issueDate=%s, hash='%s'}",
                id, studentName, course, issueDate, hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Certificate that = (Certificate) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
