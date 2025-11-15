package com.certificatevalidator.application;

/**
 * Auto launcher for the Blockchain Certificate Validator.
 * Automatically runs the best working version without requiring user input.
 */
public class AutoLauncher {
    public static void main(String[] args) {
        System.out.println("🔗 Blockchain Certificate Validator");
        System.out.println("==============================================");
        System.out.println("🚀 Auto-launching the best working version...");
        System.out.println();
        
        // Run the working demo automatically
        DemoApp demo = new DemoApp();
        demo.run();
        
        System.out.println();
        System.out.println("🎉 Thank you for using Blockchain Certificate Validator!");
        System.out.println("This project demonstrates advanced Java concepts including:");
        System.out.println("• Blockchain implementation with SHA-256 hashing");
        System.out.println("• Cryptographic security and tamper detection");
        System.out.println("• Database integration and data persistence");
        System.out.println("• Modern software architecture and design patterns");
        System.out.println();
        System.out.println("Perfect for portfolio and internship showcases! 🎓✨");
    }
}
