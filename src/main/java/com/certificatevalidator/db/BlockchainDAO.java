package com.certificatevalidator.db;

import com.certificatevalidator.blockchain.Block;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Blockchain operations.
 */
public class BlockchainDAO {
    private final DatabaseConnection dbConnection;

    public BlockchainDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Saves a block to the database
     */
    public void saveBlock(Block block) throws SQLException {
        String sql = "INSERT INTO blockchain (block_index, timestamp, certificate_hash, previous_hash, current_hash) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, block.getIndex());
            pstmt.setString(2, block.getTimestamp());
            pstmt.setString(3, block.getCertificateHash());
            pstmt.setString(4, block.getPreviousHash());
            pstmt.setString(5, block.getCurrentHash());

            pstmt.executeUpdate();
        }
    }

    /**
     * Loads all blocks from the database
     */
    public List<Block> loadAllBlocks() throws SQLException {
        List<Block> blocks = new ArrayList<>();
        String sql = "SELECT * FROM blockchain ORDER BY block_index ASC";

        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int index = rs.getInt("block_index");
                String timestamp = rs.getString("timestamp");
                String certificateHash = rs.getString("certificate_hash");
                String previousHash = rs.getString("previous_hash");
                String currentHash = rs.getString("current_hash");

                Block block = new Block(index, timestamp, certificateHash, previousHash, currentHash);
                blocks.add(block);
            }
        }
        return blocks;
    }

    /**
     * Checks if a certificate hash exists in the blockchain
     */
    public boolean containsCertificateHash(String certificateHash) throws SQLException {
        String sql = "SELECT COUNT(*) FROM blockchain WHERE certificate_hash = ?";

        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, certificateHash);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Deletes a block by certificate hash
     */
    public boolean deleteBlockByCertificateHash(String certificateHash) throws SQLException {
        String sql = "DELETE FROM blockchain WHERE certificate_hash = ? AND block_index > 0";

        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, certificateHash);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Clears all blocks from the database (except genesis)
     */
    public void clearBlocks() throws SQLException {
        String sql = "DELETE FROM blockchain WHERE block_index > 0";

        try (PreparedStatement pstmt = dbConnection.getConnection().prepareStatement(sql)) {
            pstmt.executeUpdate();
        }
    }
}

