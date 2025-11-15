package com.certificatevalidator.application;

import com.certificatevalidator.entities.Certificate;
import com.certificatevalidator.blockchain.Block;
import com.certificatevalidator.blockchain.Blockchain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo application for the Blockchain Certificate Validator.
 * This version demonstrates the core functionality without user input.
 */
public class DemoApp {
    private final Blockchain blockchain;
    private final List<Certificate> certificates;

    public DemoApp() {
        this.blockchain = new Blockchain();
        this.certificates = new ArrayList<>();
    }

    /**
     * Main demo application
     */
    public void run() {
        System.out.println("🔗 Blockchain Certificate Validator - DEMO");
        System.out.println("==============================================");
        System.out.println("📝 This demo shows the core blockchain functionality");
        System.out.println();
        
        // Demo 1: Issue some certificates
        System.out.println("📜 DEMO 1: Issuing Certificates");
        System.out.println("--------------------------------");
        issueDemoCertificates();
        
        System.out.println("\nPress Enter to continue...");
        try { System.in.read(); } catch (Exception e) {}
        
        // Demo 2: View certificates
        System.out.println("\n📋 DEMO 2: View All Certificates");
        System.out.println("---------------------------------");
        viewAllCertificates();
        
        System.out.println("\nPress Enter to continue...");
        try { System.in.read(); } catch (Exception e) {}
        
        // Demo 3: View blockchain
        System.out.println("\n🔗 DEMO 3: View Blockchain");
        System.out.println("---------------------------");
        viewBlockchain();
        
        System.out.println("\nPress Enter to continue...");
        try { System.in.read(); } catch (Exception e) {}
        
        // Demo 4: Validate certificates
        System.out.println("\n🔍 DEMO 4: Validate Certificates");
        System.out.println("---------------------------------");
        validateDemoCertificates();
        
        System.out.println("\nPress Enter to continue...");
        try { System.in.read(); } catch (Exception e) {}
        
        // Demo 5: Blockchain integrity
        System.out.println("\n🔒 DEMO 5: Blockchain Integrity Check");
        System.out.println("----------------------------------------");
        validateBlockchain();
        
        System.out.println("\n🎉 Demo completed! The blockchain certificate validator is working perfectly!");
    }

    /**
     * Issues demo certificates
     */
    private void issueDemoCertificates() {
        try {
            // Certificate 1
            Certificate cert1 = new Certificate("John Doe", "Java Programming", LocalDate.now());
            cert1.setId(1);
            certificates.add(cert1);
            Block block1 = blockchain.addBlock(cert1.getHash());
            
            System.out.println("✅ Certificate 1 issued:");
            System.out.println("   Student: " + cert1.getStudentName());
            System.out.println("   Course: " + cert1.getCourse());
            System.out.println("   Hash: " + cert1.getHash().substring(0, 20) + "...");
            System.out.println("   Block Index: " + block1.getIndex());
            
            // Certificate 2
            Certificate cert2 = new Certificate("Jane Smith", "Python Basics", LocalDate.now().plusDays(1));
            cert2.setId(2);
            certificates.add(cert2);
            Block block2 = blockchain.addBlock(cert2.getHash());
            
            System.out.println("\n✅ Certificate 2 issued:");
            System.out.println("   Student: " + cert2.getStudentName());
            System.out.println("   Course: " + cert2.getCourse());
            System.out.println("   Hash: " + cert2.getHash().substring(0, 20) + "...");
            System.out.println("   Block Index: " + block2.getIndex());
            
            // Certificate 3
            Certificate cert3 = new Certificate("Bob Johnson", "Web Development", LocalDate.now().plusDays(2));
            cert3.setId(3);
            certificates.add(cert3);
            Block block3 = blockchain.addBlock(cert3.getHash());
            
            System.out.println("\n✅ Certificate 3 issued:");
            System.out.println("   Student: " + cert3.getStudentName());
            System.out.println("   Course: " + cert3.getCourse());
            System.out.println("   Hash: " + cert3.getHash().substring(0, 20) + "...");
            System.out.println("   Block Index: " + block3.getIndex());
            
        } catch (Exception e) {
            System.out.println("❌ Error issuing certificates: " + e.getMessage());
        }
    }

    /**
     * Views all certificates
     */
    private void viewAllCertificates() {
        if (certificates.isEmpty()) {
            System.out.println("No certificates found.");
            return;
        }
        
        System.out.printf("%-5s %-20s %-20s %-12s %-20s%n", "ID", "Student", "Course", "Date", "Hash");
        System.out.println("----------------------------------------------------------------------------");
        
        for (Certificate cert : certificates) {
            System.out.printf("%-5d %-20s %-20s %-12s %-20s%n",
                cert.getId(),
                cert.getStudentName(),
                cert.getCourse(),
                cert.getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                cert.getHash().substring(0, Math.min(20, cert.getHash().length())) + "..."
            );
        }
    }

    /**
     * Views the blockchain
     */
    private void viewBlockchain() {
        List<Block> blocks = blockchain.getChain();
        
        System.out.println("Blockchain Info: " + blockchain.getChainLength() + " blocks. Chain is " + 
                         (blockchain.isChainValid() ? "valid" : "invalid"));
        System.out.println();
        
        for (Block block : blocks) {
            System.out.println("Block " + block.getIndex() + ":");
            System.out.println("  Timestamp: " + block.getTimestamp());
            System.out.println("  Certificate Hash: " + block.getCertificateHash());
            System.out.println("  Previous Hash: " + block.getPreviousHash());
            System.out.println("  Current Hash: " + block.getCurrentHash());
            System.out.println("  Valid: " + (block.isValid() ? "✅" : "❌"));
            System.out.println();
        }
    }

    /**
     * Validates demo certificates
     */
    private void validateDemoCertificates() {
        for (Certificate cert : certificates) {
            System.out.println("🔍 Validating Certificate " + cert.getId() + ":");
            System.out.println("   Student: " + cert.getStudentName());
            System.out.println("   Course: " + cert.getCourse());
            
            // Check certificate integrity
            boolean isValid = cert.isValid();
            boolean inBlockchain = blockchain.containsCertificate(cert.getHash());
            boolean chainValid = blockchain.isChainValid();
            
            if (isValid && inBlockchain && chainValid) {
                System.out.println("   Result: ✅ AUTHENTIC");
            } else {
                System.out.println("   Result: ❌ INVALID");
                if (!isValid) System.out.println("      - Certificate data tampered");
                if (!inBlockchain) System.out.println("      - Not in blockchain");
                if (!chainValid) System.out.println("      - Blockchain compromised");
            }
            System.out.println();
        }
    }

    /**
     * Validates blockchain integrity
     */
    private void validateBlockchain() {
        boolean isValid = blockchain.isChainValid();
        String status = isValid ? "✅ Blockchain is VALID" : "❌ Blockchain is INVALID";
        System.out.println(status);
        System.out.println("Blockchain Info: " + blockchain.getChainLength() + " blocks. Chain is " + 
                         (isValid ? "valid" : "invalid"));
        
        System.out.println("\n🔒 Security Features Demonstrated:");
        System.out.println("   • SHA-256 Hashing: Each certificate has a unique cryptographic hash");
        System.out.println("   • Block Chaining: Each block contains the hash of the previous block");
        System.out.println("   • Integrity Validation: Any tampering is immediately detected");
        System.out.println("   • Immutable Blocks: Once created, blocks cannot be modified");
    }

    /**
     * Main method for demo application
     */
    public static void main(String[] args) {
        DemoApp app = new DemoApp();
        app.run();
    }
}
