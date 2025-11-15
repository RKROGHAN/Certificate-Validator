package com.certificatevalidator.db;

import com.certificatevalidator.entities.Certificate;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Certificate operations.
 */
public class CertificateDAO {
    private final DatabaseConnection dbConnection;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public CertificateDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Finds the lowest available ID (for reusing deleted certificate IDs)
     * @return The lowest available ID
     */
    private int findLowestAvailableId() throws SQLException {
        // First, check if table is empty
        String countSql = "SELECT COUNT(*) as cnt FROM certificates";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(countSql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next() && rs.getInt("cnt") == 0) {
                return 1; // First certificate
            }
        }
        
        // Find gaps in the sequence
        String sql = "SELECT MIN(id + 1) AS next_id FROM certificates " +
                     "WHERE (id + 1) NOT IN (SELECT id FROM certificates WHERE id IS NOT NULL) " +
                     "AND id + 1 > 0";
        
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                Object value = rs.getObject("next_id");
                if (value != null && !rs.wasNull()) {
                    int nextId = rs.getInt("next_id");
                    if (nextId > 0) {
                        return nextId;
                    }
                }
            }
        }
        
        // If no gaps found, get the max ID and add 1
        String maxSql = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM certificates";
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(maxSql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1); // Use column index instead of name
            }
        }
        return 1; // First certificate
    }

    /**
     * Saves a certificate to the database, reusing deleted IDs if available
     * @return Certificate ID if successful, -1 if duplicate hash exists
     * @throws SQLException if database error occurs
     */
    public int saveCertificate(Certificate certificate, String filePath, String fileHash) throws SQLException {
        // Check if certificate with same hash already exists
        Certificate existing = findByHash(certificate.getHash());
        if (existing != null) {
            return -1; // Duplicate hash
        }
        
        // Find the lowest available ID
        int nextId = findLowestAvailableId();
        
        String sql = "INSERT INTO certificates (id, student_name, course, issue_date, hash, file_path, file_hash) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, nextId);
            pstmt.setString(2, certificate.getStudentName());
            pstmt.setString(3, certificate.getCourse());
            pstmt.setString(4, certificate.getIssueDate().format(DATE_FORMATTER));
            pstmt.setString(5, certificate.getHash());
            pstmt.setString(6, filePath);
            pstmt.setString(7, fileHash);

            pstmt.executeUpdate();
            return nextId;
        } catch (SQLException e) {
            // If ID already exists (shouldn't happen), try with auto-increment
            if (e.getMessage().contains("UNIQUE constraint") && e.getMessage().contains("id")) {
                sql = "INSERT INTO certificates (student_name, course, issue_date, hash, file_path, file_hash) " +
                      "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt2 = dbConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt2.setString(1, certificate.getStudentName());
                    pstmt2.setString(2, certificate.getCourse());
                    pstmt2.setString(3, certificate.getIssueDate().format(DATE_FORMATTER));
                    pstmt2.setString(4, certificate.getHash());
                    pstmt2.setString(5, filePath);
                    pstmt2.setString(6, fileHash);

                    pstmt2.executeUpdate();

                    try (ResultSet rs = pstmt2.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getInt(1);
                        }
                    }
                }
            }
            throw e;
        }
    }

    /**
     * Finds a certificate by ID
     */
    public Certificate findById(int id) throws SQLException {
        String sql = "SELECT * FROM certificates WHERE id = ?";
        
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCertificate(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a certificate by hash
     */
    public Certificate findByHash(String hash) throws SQLException {
        String sql = "SELECT * FROM certificates WHERE hash = ?";
        
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, hash);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCertificate(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a certificate by file hash
     */
    public Certificate findByFileHash(String fileHash) throws SQLException {
        String sql = "SELECT * FROM certificates WHERE file_hash = ?";
        
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, fileHash);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCertificate(rs);
                }
            }
        }
        return null;
    }

    /**
     * Gets all certificates
     */
    public List<Certificate> findAll() throws SQLException {
        List<Certificate> certificates = new ArrayList<>();
        String sql = "SELECT * FROM certificates ORDER BY id DESC";
        
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                certificates.add(mapResultSetToCertificate(rs));
            }
        }
        return certificates;
    }

    /**
     * Gets file path for a certificate
     */
    public String getFilePath(int certificateId) throws SQLException {
        String sql = "SELECT file_path FROM certificates WHERE id = ?";
        
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, certificateId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("file_path");
                }
            }
        }
        return null;
    }

    /**
     * Deletes a certificate by ID
     * @param id Certificate ID to delete
     * @return true if certificate was deleted, false if not found
     * @throws SQLException if database error occurs
     */
    public boolean deleteCertificate(int id) throws SQLException {
        String sql = "DELETE FROM certificates WHERE id = ?";
        
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Gets certificate with file path for deletion
     */
    public CertificateWithFile getCertificateWithFile(int id) throws SQLException {
        String sql = "SELECT id, student_name, course, issue_date, hash, file_path FROM certificates WHERE id = ?";
        
        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int certId = rs.getInt("id");
                    String studentName = rs.getString("student_name");
                    String course = rs.getString("course");
                    LocalDate issueDate = LocalDate.parse(rs.getString("issue_date"), DATE_FORMATTER);
                    String hash = rs.getString("hash");
                    String filePath = rs.getString("file_path");
                    
                    return new CertificateWithFile(certId, studentName, course, issueDate, hash, filePath);
                }
            }
        }
        return null;
    }

    /**
     * Helper class to hold certificate with file path
     */
    public static class CertificateWithFile {
        private final int id;
        private final String studentName;
        private final String course;
        private final LocalDate issueDate;
        private final String hash;
        private final String filePath;

        public CertificateWithFile(int id, String studentName, String course, LocalDate issueDate, String hash, String filePath) {
            this.id = id;
            this.studentName = studentName;
            this.course = course;
            this.issueDate = issueDate;
            this.hash = hash;
            this.filePath = filePath;
        }

        public int getId() { return id; }
        public String getStudentName() { return studentName; }
        public String getCourse() { return course; }
        public LocalDate getIssueDate() { return issueDate; }
        public String getHash() { return hash; }
        public String getFilePath() { return filePath; }
    }

    /**
     * Maps ResultSet to Certificate object
     */
    private Certificate mapResultSetToCertificate(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String studentName = rs.getString("student_name");
        String course = rs.getString("course");
        LocalDate issueDate = LocalDate.parse(rs.getString("issue_date"), DATE_FORMATTER);
        String hash = rs.getString("hash");
        
        return new Certificate(id, studentName, course, issueDate, hash);
    }
}

