package com.certificatevalidator.application;

import com.certificatevalidator.web.SimpleWebServer;

/**
 * Simple web application launcher for the Blockchain Certificate Validator.
 * Works without database dependencies for easy deployment.
 */
public class SimpleWebApp {
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Check for custom port argument
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default port " + DEFAULT_PORT);
            }
        }

        System.out.println("🌐 Blockchain Certificate Validator - Simple Web Interface");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("🚀 Starting simple web server...");
        System.out.println("📱 Open your browser and go to: http://localhost:" + port);
        System.out.println("🔗 Features:");
        System.out.println("   • 📜 Issue certificates with web form");
        System.out.println("   • 🔍 Validate certificates by ID or hash");
        System.out.println("   • 📋 View all certificates in a table");
        System.out.println("   • 🔗 Explore the blockchain");
        System.out.println("   • 📱 Responsive design for mobile and desktop");
        System.out.println("   • 💾 In-memory storage (no database required)");
        System.out.println();
        System.out.println("💡 Press Ctrl+C to stop the server");
        System.out.println();

        SimpleWebServer server = new SimpleWebServer(port);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutting down web server...");
            server.stop();
        }));

        try {
            server.start();
        } catch (Exception e) {
            System.err.println("❌ Error starting web server: " + e.getMessage());
            System.exit(1);
        }
    }
}
