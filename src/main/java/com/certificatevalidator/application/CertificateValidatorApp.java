package com.certificatevalidator.application;

import com.certificatevalidator.entities.Certificate;
import com.certificatevalidator.blockchain.Block;
import com.certificatevalidator.blockchain.Blockchain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Main application for the Blockchain Certificate Validator.
 * Provides a clean, professional interface for certificate management.
 */
public class CertificateValidatorApp {
    private final Blockchain blockchain;
    private final List<Certificate> certificates;
    private final Scanner scanner;

    public CertificateValidatorApp() {
        this.blockchain = new Blockchain();
        this.certificates = new ArrayList<>();
        this.scanner = new Scanner(System.in);
    }

    /**
     * Main application entry point
     */
    public static void main(String[] args) {
        CertificateValidatorApp app = new CertificateValidatorApp();
        app.run();
    }

    /**
     * Main application loop
     */
    public void run() {
        displayWelcome();
        
        while (true) {
            displayMainMenu();
            int choice = getMenuChoice();
            
            switch (choice) {
                case 1:
                    issueCertificate();
                    break;
                case 2:
                    validateCertificate();
                    break;
                case 3:
                    viewAllCertificates();
                    break;
                case 4:
                    viewBlockchain();
                    break;
                case 5:
                    validateBlockchain();
                    break;
                case 6:
                    runDemo();
                    break;
                case 7:
                    System.out.println("\nThank you for using Blockchain Certificate Validator!");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    break;
            }
            
            pauseForUser();
        }
    }

    /**
     * Displays welcome message
     */
    private void displayWelcome() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                🔗 Blockchain Certificate Validator          ║");
        System.out.println("║                                                              ║");
        System.out.println("║  A secure, blockchain-based certificate validation system   ║");
        System.out.println("║  featuring SHA-256 hashing and tamper detection.            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Displays the main menu
     */
    private void displayMainMenu() {
        System.out.println("\n📋 MAIN MENU");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("1. 📜 Issue Certificate");
        System.out.println("2. 🔍 Validate Certificate");
        System.out.println("3. 📋 View All Certificates");
        System.out.println("4. 🔗 View Blockchain");
        System.out.println("5. 🔒 Validate Blockchain Integrity");
        System.out.println("6. 🎬 Run Demo");
        System.out.println("7. 🚪 Exit");
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    /**
     * Gets user menu choice
     */
    private int getMenuChoice() {
        while (true) {
            try {
                System.out.print("Enter your choice (1-7): ");
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= 7) {
                    return choice;
                } else {
                    System.out.println("Please enter a number between 1 and 7.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            } catch (Exception e) {
                System.out.println("Input error. Please try again.");
                return 7; // Exit on error
            }
        }
    }

    /**
     * Issues a new certificate
     */
    private void issueCertificate() {
        System.out.println("\n📜 ISSUE NEW CERTIFICATE");
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        try {
            String studentName = getInput("Enter student name: ");
            String course = getInput("Enter course name: ");
            String dateStr = getInput("Enter issue date (YYYY-MM-DD) or press Enter for today: ");
            
            LocalDate issueDate;
            if (dateStr.isEmpty()) {
                issueDate = LocalDate.now();
            } else {
                issueDate = LocalDate.parse(dateStr);
            }
            
            // Create certificate
            Certificate certificate = new Certificate(studentName, course, issueDate);
            certificate.setId(certificates.size() + 1);
            certificates.add(certificate);
            
            // Add to blockchain
            Block newBlock = blockchain.addBlock(certificate.getHash());
            
            System.out.println("\n✅ CERTIFICATE ISSUED SUCCESSFULLY!");
            System.out.println("───────────────────────────────────────────────────────────");
            System.out.println("Certificate ID: " + certificate.getId());
            System.out.println("Student: " + certificate.getStudentName());
            System.out.println("Course: " + certificate.getCourse());
            System.out.println("Issue Date: " + certificate.getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            System.out.println("Hash: " + certificate.getHash());
            System.out.println("Block Index: " + newBlock.getIndex());
            
        } catch (Exception e) {
            System.out.println("❌ Error issuing certificate: " + e.getMessage());
        }
    }

    /**
     * Validates a certificate
     */
    private void validateCertificate() {
        System.out.println("\n🔍 VALIDATE CERTIFICATE");
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        if (certificates.isEmpty()) {
            System.out.println("No certificates found. Please issue some certificates first.");
            return;
        }
        
        System.out.println("Validation Options:");
        System.out.println("1. Validate by Certificate ID");
        System.out.println("2. Validate by Certificate Hash");
        System.out.print("Choose option (1-2): ");
        
        try {
            int option = Integer.parseInt(scanner.nextLine().trim());
            
            if (option == 1) {
                validateById();
            } else if (option == 2) {
                validateByHash();
            } else {
                System.out.println("Invalid option.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /**
     * Validates certificate by ID
     */
    private void validateById() {
        try {
            int certificateId = Integer.parseInt(getInput("Enter certificate ID: "));
            
            if (certificateId < 1 || certificateId > certificates.size()) {
                System.out.println("❌ Certificate not found");
                return;
            }
            
            Certificate certificate = certificates.get(certificateId - 1);
            displayValidationResult(certificate);
            
        } catch (NumberFormatException e) {
            System.out.println("❌ Please enter a valid certificate ID");
        }
    }

    /**
     * Validates certificate by hash
     */
    private void validateByHash() {
        String hash = getInput("Enter certificate hash: ");
        
        Certificate certificate = null;
        for (Certificate cert : certificates) {
            if (cert.getHash().equals(hash)) {
                certificate = cert;
                break;
            }
        }
        
        if (certificate == null) {
            System.out.println("❌ Certificate not found");
            return;
        }
        
        displayValidationResult(certificate);
    }

    /**
     * Displays validation result
     */
    private void displayValidationResult(Certificate certificate) {
        System.out.println("\n📊 VALIDATION RESULT");
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        boolean isValid = certificate.isValid();
        boolean inBlockchain = blockchain.containsCertificate(certificate.getHash());
        boolean chainValid = blockchain.isChainValid();
        
        if (isValid && inBlockchain && chainValid) {
            System.out.println("✅ CERTIFICATE IS AUTHENTIC");
        } else {
            System.out.println("❌ CERTIFICATE IS INVALID");
            if (!isValid) System.out.println("   • Certificate data has been tampered with");
            if (!inBlockchain) System.out.println("   • Certificate not found in blockchain");
            if (!chainValid) System.out.println("   • Blockchain integrity compromised");
        }
        
        System.out.println("\n📜 Certificate Details:");
        System.out.println(certificate.getCertificateInfo());
        
        Block block = blockchain.getBlockByCertificateHash(certificate.getHash());
        if (block != null) {
            System.out.println("\n🔗 Blockchain Block Details:");
            System.out.println("Block Index: " + block.getIndex());
            System.out.println("Timestamp: " + block.getTimestamp());
            System.out.println("Certificate Hash: " + block.getCertificateHash());
            System.out.println("Previous Hash: " + block.getPreviousHash());
            System.out.println("Current Hash: " + block.getCurrentHash());
        }
    }

    /**
     * Views all certificates
     */
    private void viewAllCertificates() {
        System.out.println("\n📋 ALL CERTIFICATES");
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        if (certificates.isEmpty()) {
            System.out.println("No certificates found.");
            return;
        }
        
        System.out.printf("%-5s %-20s %-25s %-12s %-20s%n", "ID", "Student", "Course", "Date", "Hash");
        System.out.println("─────────────────────────────────────────────────────────────────────────────────────");
        
        for (Certificate cert : certificates) {
            System.out.printf("%-5d %-20s %-25s %-12s %-20s%n",
                cert.getId(),
                cert.getStudentName(),
                cert.getCourse(),
                cert.getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                cert.getHash().substring(0, Math.min(20, cert.getHash().length())) + "..."
            );
        }
        
        System.out.println("\nTotal Certificates: " + certificates.size());
    }

    /**
     * Views the blockchain
     */
    private void viewBlockchain() {
        System.out.println("\n🔗 BLOCKCHAIN EXPLORER");
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        List<Block> blocks = blockchain.getChain();
        
        if (blocks.isEmpty()) {
            System.out.println("Blockchain is empty.");
            return;
        }
        
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
     * Validates blockchain integrity
     */
    private void validateBlockchain() {
        System.out.println("\n🔒 BLOCKCHAIN INTEGRITY CHECK");
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        boolean isValid = blockchain.isChainValid();
        String status = isValid ? "✅ BLOCKCHAIN IS VALID" : "❌ BLOCKCHAIN IS INVALID";
        System.out.println(status);
        System.out.println("Blockchain Info: " + blockchain.getChainLength() + " blocks. Chain is " + 
                         (isValid ? "valid" : "invalid"));
        
        if (isValid) {
            System.out.println("\n🔒 Security Features Active:");
            System.out.println("   • SHA-256 Hashing: Each certificate has a unique cryptographic hash");
            System.out.println("   • Block Chaining: Each block contains the hash of the previous block");
            System.out.println("   • Integrity Validation: Any tampering is immediately detected");
            System.out.println("   • Immutable Blocks: Once created, blocks cannot be modified");
        }
    }

    /**
     * Runs the automated demo
     */
    private void runDemo() {
        System.out.println("\n🎬 RUNNING AUTOMATED DEMO");
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        DemoApp demo = new DemoApp();
        demo.run();
    }

    /**
     * Gets string input from user
     */
    private String getInput(String prompt) {
        System.out.print(prompt);
        try {
            return scanner.nextLine().trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Pauses for user input
     */
    private void pauseForUser() {
        System.out.println("\nPress Enter to continue...");
        try {
            scanner.nextLine();
        } catch (Exception e) {
            // Handle case where scanner might not work
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
