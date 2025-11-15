package com.certificatevalidator.web;

import com.certificatevalidator.entities.Certificate;
import com.certificatevalidator.blockchain.Block;
import com.certificatevalidator.blockchain.Blockchain;
import com.certificatevalidator.blockchain.HashUtils;
import com.certificatevalidator.db.BlockchainDAO;
import com.certificatevalidator.db.CertificateDAO;
import com.certificatevalidator.db.DatabaseConnection;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple web server for the Blockchain Certificate Validator.
 * Now with database persistence and file upload support.
 */
public class SimpleWebServer {
    private final int port;
    private final Blockchain blockchain;
    private final CertificateDAO certificateDAO;
    private final BlockchainDAO blockchainDAO;
    private final ExecutorService executorService;
    private final Path uploadsDirectory;
    private volatile boolean running = false;

    public SimpleWebServer(int port) {
        this.port = port;
        this.blockchain = new Blockchain();
        this.certificateDAO = new CertificateDAO();
        this.blockchainDAO = new BlockchainDAO();
        this.executorService = Executors.newFixedThreadPool(10);
        
        // Create uploads directory
        this.uploadsDirectory = Paths.get("uploads");
        try {
            Files.createDirectories(uploadsDirectory);
        } catch (IOException e) {
            System.err.println("Failed to create uploads directory: " + e.getMessage());
        }
        
        // Load existing data from database
        loadFromDatabase();
    }

    /**
     * Loads certificates and blockchain from database
     */
    private void loadFromDatabase() {
        try {
            // Load certificates
            List<Certificate> certs = certificateDAO.findAll();
            System.out.println("📦 Loaded " + certs.size() + " certificates from database");
            
            // Load blockchain blocks
            List<Block> blocks = blockchainDAO.loadAllBlocks();
            if (blocks.size() > 1) { // Skip genesis block
                for (int i = 1; i < blocks.size(); i++) {
                    blockchain.addExistingBlock(blocks.get(i));
                }
                System.out.println("🔗 Loaded " + (blocks.size() - 1) + " blocks from blockchain");
            }
        } catch (SQLException e) {
            System.err.println("Error loading from database: " + e.getMessage());
        }
    }

    /**
     * Starts the web server
     */
    public void start() {
        running = true;
        System.out.println("🌐 Starting Simple Web Server on port " + port);
        System.out.println("📱 Open your browser and go to: http://localhost:" + port);
        System.out.println("🔗 Blockchain Certificate Validator Web Interface");
        System.out.println("💾 Using SQLite database for permanent storage");
        System.out.println("📁 Upload directory: " + uploadsDirectory.toAbsolutePath());
        System.out.println();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error starting web server: " + e.getMessage());
            }
        }
    }

    /**
     * Stops the web server
     */
    public void stop() {
        running = false;
        executorService.shutdown();
        DatabaseConnection.getInstance().close();
        System.out.println("Web server stopped.");
    }

    /**
     * Handles individual client requests
     */
    private class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String requestLine = in.readLine();
                if (requestLine == null) return;

                String[] requestParts = requestLine.split(" ");
                String method = requestParts[0];
                String path = requestParts[1];

                if ("GET".equals(method)) {
                    handleGetRequest(path, out);
                } else if ("POST".equals(method)) {
                    handlePostRequest(in, out);
                } else if ("DELETE".equals(method)) {
                    handleDeleteRequest(path, out);
                }

            } catch (IOException e) {
                System.err.println("Error handling client request: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }

        /**
         * Handles GET requests
         */
        private void handleGetRequest(String path, PrintWriter out) throws IOException {
            if ("/".equals(path) || "/index.html".equals(path)) {
                serveHtmlPage(out);
            } else if ("/style.css".equals(path)) {
                serveCssFile(out);
            } else if ("/script.js".equals(path)) {
                serveJavaScriptFile(out);
            } else if ("/api/certificates".equals(path)) {
                serveCertificatesApi(out);
            } else if ("/api/blockchain".equals(path)) {
                serveBlockchainApi(out);
            } else if (path.startsWith("/api/certificates/delete/")) {
                String idStr = path.substring("/api/certificates/delete/".length());
                try {
                    int id = Integer.parseInt(idStr);
                    handleDeleteCertificate(id, out);
                } catch (NumberFormatException e) {
                    serve400(out, "Invalid certificate ID");
                }
            } else if (path.startsWith("/api/certificates/download/")) {
                String idStr = path.substring("/api/certificates/download/".length());
                try {
                    int id = Integer.parseInt(idStr);
                    handleDownloadCertificate(id, clientSocket);
                } catch (NumberFormatException e) {
                    serve400(out, "Invalid certificate ID");
                }
            } else {
                serve404(out);
            }
        }
        
        /**
         * Handles DELETE requests
         */
        private void handleDeleteRequest(String path, PrintWriter out) throws IOException {
            if (path.startsWith("/api/certificates/delete/")) {
                String idStr = path.substring("/api/certificates/delete/".length());
                try {
                    int id = Integer.parseInt(idStr);
                    handleDeleteCertificate(id, out);
                } catch (NumberFormatException e) {
                    serve400(out, "Invalid certificate ID");
                }
            } else {
                serve404(out);
            }
        }
        
        /**
         * Handles certificate deletion
         */
        private void handleDeleteCertificate(int certificateId, PrintWriter out) {
            try {
                // Get certificate with file path before deleting
                CertificateDAO.CertificateWithFile certWithFile = certificateDAO.getCertificateWithFile(certificateId);
                
                if (certWithFile == null) {
                    serve400(out, "Certificate not found");
                    return;
                }
                
                String certificateHash = certWithFile.getHash();
                
                // Delete the certificate from database
                boolean deleted = certificateDAO.deleteCertificate(certificateId);
                
                if (!deleted) {
                    serve400(out, "Failed to delete certificate");
                    return;
                }
                
                // Remove from blockchain (both in-memory and database)
                blockchain.removeBlockByCertificateHash(certificateHash);
                blockchainDAO.deleteBlockByCertificateHash(certificateHash);
                
                // Delete associated file if it exists
                if (certWithFile.getFilePath() != null && !certWithFile.getFilePath().isEmpty()) {
                    try {
                        java.nio.file.Path filePath = Paths.get(certWithFile.getFilePath());
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                        }
                    } catch (IOException e) {
                        System.err.println("Warning: Could not delete file: " + e.getMessage());
                        // Continue even if file deletion fails
                    }
                }
                
                String response = "{\"success\": true, \"message\": \"Certificate deleted successfully\", \"certificateId\": " + certificateId + "}";
                
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println("Access-Control-Allow-Origin: *");
                out.println("Content-Length: " + response.getBytes().length);
                out.println();
                out.println(response);
                
            } catch (SQLException e) {
                serve500(out, "Database error: " + e.getMessage());
            } catch (Exception e) {
                serve500(out, "Error deleting certificate: " + e.getMessage());
            }
        }

        /**
         * Handles certificate file download
         */
        private void handleDownloadCertificate(int certificateId, Socket socket) throws IOException {
            try {
                CertificateDAO.CertificateWithFile certWithFile = certificateDAO.getCertificateWithFile(certificateId);
                
                if (certWithFile == null) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    serve400(out, "Certificate not found");
                    return;
                }
                
                String filePath = certWithFile.getFilePath();
                if (filePath == null || filePath.isEmpty()) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    serve400(out, "No file associated with this certificate");
                    return;
                }
                
                java.nio.file.Path path = Paths.get(filePath);
                if (!Files.exists(path)) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    serve400(out, "Certificate file not found on disk");
                    return;
                }
                
                // Read file
                byte[] fileBytes = Files.readAllBytes(path);
                String filename = path.getFileName().toString();
                // Remove timestamp prefix if present
                if (filename.contains("_")) {
                    filename = filename.substring(filename.indexOf("_") + 1);
                }
                
                // Get output stream
                java.io.OutputStream outputStream = socket.getOutputStream();
                PrintWriter headerWriter = new PrintWriter(outputStream, true);
                
                // Send headers
                headerWriter.println("HTTP/1.1 200 OK");
                headerWriter.println("Content-Type: application/octet-stream");
                headerWriter.println("Content-Disposition: attachment; filename=\"" + filename + "\"");
                headerWriter.println("Content-Length: " + fileBytes.length);
                headerWriter.println("Access-Control-Allow-Origin: *");
                headerWriter.println();
                headerWriter.flush();
                
                // Write file bytes directly to output stream
                outputStream.write(fileBytes);
                outputStream.flush();
                
            } catch (SQLException e) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                serve500(out, "Database error: " + e.getMessage());
            } catch (Exception e) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                serve500(out, "Error downloading certificate: " + e.getMessage());
            }
        }

        /**
         * Handles POST requests
         */
        private void handlePostRequest(BufferedReader in, PrintWriter out) throws IOException {
            // Read headers
            String line;
            int contentLength = 0;
            String contentType = null;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                String lowerLine = line.toLowerCase();
                if (lowerLine.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                } else if (lowerLine.startsWith("content-type:")) {
                    contentType = line.substring(14).trim();
                }
            }

            // Read the request body
            byte[] bodyBytes = new byte[contentLength];
            int bytesRead = 0;
            while (bytesRead < contentLength) {
                int read = in.read();
                if (read == -1) break;
                bodyBytes[bytesRead++] = (byte) read;
            }

            // Check if it's multipart form data
            if (contentType != null && contentType.contains("multipart/form-data")) {
                handleMultipartRequest(contentType, bodyBytes, out);
            } else {
                // Regular form data
                String requestBody = new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
                if (requestBody.contains("action=issue")) {
                    handleIssueCertificate(requestBody, out);
                } else if (requestBody.contains("action=validate")) {
                    handleValidateCertificate(requestBody, out);
                } else {
                    serve404(out);
                }
            }
        }
        
        /**
         * Handles multipart form data requests (file uploads)
         */
        private void handleMultipartRequest(String contentType, byte[] bodyBytes, PrintWriter out) throws IOException {
            try {
                MultipartParser parser = new MultipartParser(contentType, bodyBytes);
                MultipartParser.MultipartData data = parser.parse();
                
                String action = data.getField("action");
                
                if ("issue".equals(action)) {
                    handleIssueCertificateWithFile(data, out);
                } else if ("validate".equals(action)) {
                    handleValidateCertificateWithFile(data, out);
                } else {
                    serve404(out);
                }
            } catch (Exception e) {
                serve500(out, "Error processing file upload: " + e.getMessage());
            }
        }

        /**
         * Serves the main HTML page
         */
        private void serveHtmlPage(PrintWriter out) {
            String html = getHtmlContent();
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println("Content-Length: " + html.getBytes().length);
            out.println();
            out.println(html);
        }

        /**
         * Serves CSS file
         */
        private void serveCssFile(PrintWriter out) {
            String css = getCssContent();
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/css");
            out.println("Content-Length: " + css.getBytes().length);
            out.println();
            out.println(css);
        }

        /**
         * Serves JavaScript file
         */
        private void serveJavaScriptFile(PrintWriter out) {
            String js = getJavaScriptContent();
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: application/javascript");
            out.println("Content-Length: " + js.getBytes().length);
            out.println();
            out.println(js);
        }

        /**
         * Serves certificates API
         */
        private void serveCertificatesApi(PrintWriter out) {
            String json = getCertificatesJson();
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: application/json");
            out.println("Access-Control-Allow-Origin: *");
            out.println("Content-Length: " + json.getBytes().length);
            out.println();
            out.println(json);
        }

        /**
         * Serves blockchain API
         */
        private void serveBlockchainApi(PrintWriter out) {
            String json = getBlockchainJson();
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: application/json");
            out.println("Access-Control-Allow-Origin: *");
            out.println("Content-Length: " + json.getBytes().length);
            out.println();
            out.println(json);
        }

        /**
         * Handles certificate issuance
         */
        private void handleIssueCertificate(String requestBody, PrintWriter out) {
            try {
                Map<String, String> params = parseFormData(requestBody);
                String studentName = params.get("studentName");
                String course = params.get("course");
                String issueDate = params.get("issueDate");

                if (studentName == null || course == null || issueDate == null) {
                    serve400(out, "Missing required fields");
                    return;
                }

                LocalDate date = LocalDate.parse(issueDate);
                
                // Create certificate
                Certificate certificate = new Certificate(studentName, course, date);
                
                // Check if certificate already exists
                Certificate existing = certificateDAO.findByHash(certificate.getHash());
                if (existing != null) {
                    serve400(out, "Certificate with this data already exists. Hash: " + certificate.getHash());
                    return;
                }
                
                // Save to database
                int certificateId = certificateDAO.saveCertificate(certificate, null, null);
                if (certificateId == -1) {
                    serve400(out, "Certificate with this hash already exists");
                    return;
                }
                certificate.setId(certificateId);
                
                // Add to blockchain
                Block newBlock = blockchain.addBlock(certificate.getHash());
                blockchainDAO.saveBlock(newBlock);

                String response = "{\"success\": true, \"certificateId\": " + certificate.getId() + 
                                ", \"hash\": \"" + certificate.getHash() + "\", \"blockIndex\": " + newBlock.getIndex() + "}";
                
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println("Access-Control-Allow-Origin: *");
                out.println("Content-Length: " + response.getBytes().length);
                out.println();
                out.println(response);

            } catch (SQLException e) {
                if (e.getMessage().contains("UNIQUE constraint")) {
                    serve400(out, "Certificate with this data already exists");
                } else {
                    serve500(out, "Database error: " + e.getMessage());
                }
            } catch (Exception e) {
                serve500(out, e.getMessage());
            }
        }
        
        /**
         * Handles certificate issuance with file upload
         */
        private void handleIssueCertificateWithFile(MultipartParser.MultipartData data, PrintWriter out) {
            try {
                String studentName = data.getField("studentName");
                String course = data.getField("course");
                String issueDate = data.getField("issueDate");

                if (studentName == null || course == null || issueDate == null) {
                    serve400(out, "Missing required fields");
                    return;
                }

                LocalDate date = LocalDate.parse(issueDate);
                
                // Create certificate
                Certificate certificate = new Certificate(studentName, course, date);
                
                // Handle file upload if present
                String filePath = null;
                String fileHash = null;
                if (data.hasFile("certificateFile")) {
                    MultipartParser.MultipartData.FileData fileData = data.getFile("certificateFile");
                    if (fileData.getData().length > 0) {
                        // Generate hash from file content
                        fileHash = HashUtils.hashBytes(fileData.getData());
                        
                        // Save file to disk
                        String filename = System.currentTimeMillis() + "_" + fileData.getFilename();
                        filePath = uploadsDirectory.resolve(filename).toString();
                        Files.write(Paths.get(filePath), fileData.getData());
                    }
                }
                
                // Check if certificate already exists
                Certificate existing = certificateDAO.findByHash(certificate.getHash());
                if (existing != null) {
                    serve400(out, "Certificate with this data already exists. Hash: " + certificate.getHash());
                    return;
                }
                
                // Save to database
                int certificateId = certificateDAO.saveCertificate(certificate, filePath, fileHash);
                if (certificateId == -1) {
                    serve400(out, "Certificate with this hash already exists");
                    return;
                }
                certificate.setId(certificateId);
                
                // Add to blockchain
                Block newBlock = blockchain.addBlock(certificate.getHash());
                blockchainDAO.saveBlock(newBlock);

                String response = "{\"success\": true, \"certificateId\": " + certificate.getId() + 
                                ", \"hash\": \"" + certificate.getHash() + "\"" +
                                (fileHash != null ? ", \"fileHash\": \"" + fileHash + "\"" : "") +
                                ", \"blockIndex\": " + newBlock.getIndex() + "}";
                
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println("Access-Control-Allow-Origin: *");
                out.println("Content-Length: " + response.getBytes().length);
                out.println();
                out.println(response);

            } catch (SQLException e) {
                if (e.getMessage().contains("UNIQUE constraint")) {
                    serve400(out, "Certificate with this data already exists");
                } else {
                    serve500(out, "Database error: " + e.getMessage());
                }
            } catch (Exception e) {
                serve500(out, e.getMessage());
            }
        }

        /**
         * Handles certificate validation
         */
        private void handleValidateCertificate(String requestBody, PrintWriter out) {
            try {
                Map<String, String> params = parseFormData(requestBody);
                String certificateId = params.get("certificateId");
                String hash = params.get("hash");

                Certificate certificate = null;
                if (certificateId != null && !certificateId.isEmpty()) {
                    int id = Integer.parseInt(certificateId);
                    certificate = certificateDAO.findById(id);
                } else if (hash != null && !hash.isEmpty()) {
                    certificate = certificateDAO.findByHash(hash);
                }

                if (certificate == null) {
                    serve400(out, "Certificate not found");
                    return;
                }

                // Validate certificate
                boolean isValid = certificate.isValid();
                boolean inBlockchain = blockchain.containsCertificate(certificate.getHash());
                // Use lenient chain validation that allows for deleted blocks
                boolean chainValid = inBlockchain ? blockchain.isChainValidLenient() : true;
                boolean overallValid = isValid && inBlockchain && chainValid;

                String message = overallValid ? "Certificate is authentic" : "Certificate is invalid";
                if (!isValid) message += " - Data tampered";
                if (!inBlockchain) message += " - Not in blockchain";
                if (inBlockchain && !chainValid) message += " - Blockchain compromised";

                String response = "{\"success\": true, \"valid\": " + overallValid + 
                                ", \"message\": \"" + message + "\", \"certificateId\": " + certificate.getId() + "}";
                
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println("Access-Control-Allow-Origin: *");
                out.println("Content-Length: " + response.getBytes().length);
                out.println();
                out.println(response);

            } catch (Exception e) {
                serve500(out, e.getMessage());
            }
        }
        
        /**
         * Handles certificate validation with file upload
         */
        private void handleValidateCertificateWithFile(MultipartParser.MultipartData data, PrintWriter out) {
            try {
                String certificateId = data.getField("certificateId");
                String hash = data.getField("hash");
                Certificate certificate = null;
                
                // Check if file was uploaded
                if (data.hasFile("certificateFile")) {
                    MultipartParser.MultipartData.FileData fileData = data.getFile("certificateFile");
                    if (fileData.getData().length > 0) {
                        // Generate hash from uploaded file
                        String uploadedFileHash = HashUtils.hashBytes(fileData.getData());
                        
                        // Find certificate by file hash
                        certificate = certificateDAO.findByFileHash(uploadedFileHash);
                        
                        if (certificate == null) {
                            serve400(out, "Certificate not found - File hash does not match any stored certificate");
                            return;
                        }
                    }
                }
                
                // Fallback to ID or hash if no file
                if (certificate == null) {
                    if (certificateId != null && !certificateId.isEmpty()) {
                        int id = Integer.parseInt(certificateId);
                        certificate = certificateDAO.findById(id);
                    } else if (hash != null && !hash.isEmpty()) {
                        certificate = certificateDAO.findByHash(hash);
                    }
                }

                if (certificate == null) {
                    serve400(out, "Certificate not found");
                    return;
                }

                // Validate certificate
                boolean isValid = certificate.isValid();
                boolean inBlockchain = blockchain.containsCertificate(certificate.getHash());
                // Use lenient chain validation that allows for deleted blocks
                boolean chainValid = inBlockchain ? blockchain.isChainValidLenient() : true;
                boolean overallValid = isValid && inBlockchain && chainValid;

                String message = overallValid ? "Certificate is authentic" : "Certificate is invalid";
                if (!isValid) message += " - Data tampered";
                if (!inBlockchain) message += " - Not in blockchain";
                if (inBlockchain && !chainValid) message += " - Blockchain compromised";

                String response = "{\"success\": true, \"valid\": " + overallValid + 
                                ", \"message\": \"" + message + "\", \"certificateId\": " + certificate.getId() + 
                                ", \"hash\": \"" + certificate.getHash() + "\"}";
                
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: application/json");
                out.println("Access-Control-Allow-Origin: *");
                out.println("Content-Length: " + response.getBytes().length);
                out.println();
                out.println(response);

            } catch (Exception e) {
                serve500(out, e.getMessage());
            }
        }

        /**
         * Serves 404 error
         */
        private void serve404(PrintWriter out) {
            String response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\n404 Not Found";
            out.println(response);
        }

        /**
         * Serves 400 error
         */
        private void serve400(PrintWriter out, String message) {
            String response = "{\"success\": false, \"error\": \"" + message + "\"}";
            out.println("HTTP/1.1 400 Bad Request");
            out.println("Content-Type: application/json");
            out.println("Content-Length: " + response.getBytes().length);
            out.println();
            out.println(response);
        }

        /**
         * Serves 500 error
         */
        private void serve500(PrintWriter out, String message) {
            String response = "{\"success\": false, \"error\": \"" + message + "\"}";
            out.println("HTTP/1.1 500 Internal Server Error");
            out.println("Content-Type: application/json");
            out.println("Content-Length: " + response.getBytes().length);
            out.println();
            out.println(response);
        }

        /**
         * Parses form data
         */
        private Map<String, String> parseFormData(String formData) {
            Map<String, String> params = new HashMap<>();
            String[] pairs = formData.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1].replace("+", " "));
                }
            }
            return params;
        }
    }

    /**
     * Gets HTML content
     */
    private String getHtmlContent() {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Blockchain Certificate Validator | Enterprise Solution</title>
            <link rel="stylesheet" href="style.css">
            <link rel="preconnect" href="https://fonts.googleapis.com">
            <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
            <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
        </head>
        <body>
            <div class="container">
                <header>
                    <div class="header-content">
                        <div class="logo">
                            <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <rect width="48" height="48" rx="12" fill="url(#gradient)"/>
                                <path d="M24 14L30 18V28L24 32L18 28V18L24 14Z" stroke="white" stroke-width="2" fill="none"/>
                                <path d="M24 20L26 21V25L24 26L22 25V21L24 20Z" fill="white"/>
                                <defs>
                                    <linearGradient id="gradient" x1="0" y1="0" x2="48" y2="48">
                                        <stop offset="0%" stop-color="#667eea"/>
                                        <stop offset="100%" stop-color="#764ba2"/>
                                    </linearGradient>
                                </defs>
                            </svg>
                        </div>
                        <div class="header-text">
                            <h1>Blockchain Certificate Validator</h1>
                            <p>Enterprise-grade certificate validation powered by blockchain technology</p>
                        </div>
                    </div>
                    <div class="status-badge">
                        <span class="badge-icon">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                        </span>
                        <span>Secure Database Storage</span>
                    </div>
                </header>

                <nav class="tabs">
                    <button class="tab-button active" onclick="showTab('issue')">
                        <span class="tab-icon">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                                <line x1="16" y1="13" x2="8" y2="13"></line>
                                <line x1="16" y1="17" x2="8" y2="17"></line>
                                <polyline points="10 9 9 9 8 9"></polyline>
                            </svg>
                        </span>
                        <span class="tab-text">Issue Certificate</span>
                    </button>
                    <button class="tab-button" onclick="showTab('validate')">
                        <span class="tab-icon">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                                <polyline points="9 12 11 14 15 10"></polyline>
                            </svg>
                        </span>
                        <span class="tab-text">Validate Certificate</span>
                    </button>
                    <button class="tab-button" onclick="showTab('view')">
                        <span class="tab-icon">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                <line x1="9" y1="3" x2="9" y2="21"></line>
                                <line x1="3" y1="9" x2="21" y2="9"></line>
                            </svg>
                        </span>
                        <span class="tab-text">View Certificates</span>
                    </button>
                    <button class="tab-button" onclick="showTab('blockchain')">
                        <span class="tab-icon">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                            </svg>
                        </span>
                        <span class="tab-text">Blockchain Explorer</span>
                    </button>
                </nav>

                <main>
                    <!-- Issue Certificate Tab -->
                    <div id="issue" class="tab-content active">
                        <div class="tab-header">
                            <h2>Issue New Certificate</h2>
                            <p class="tab-description">Create and register a new certificate in the blockchain system</p>
                        </div>
                        <form id="issueForm" class="form" enctype="multipart/form-data">
                            <div class="form-group">
                                <label for="studentName">
                                    <span class="label-icon">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                            <circle cx="12" cy="7" r="4"></circle>
                                        </svg>
                                    </span>
                                    Student Name
                                </label>
                                <input type="text" id="studentName" name="studentName" placeholder="Enter student's full name" required>
                            </div>
                            <div class="form-group">
                                <label for="course">
                                    <span class="label-icon">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"></path>
                                            <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"></path>
                                        </svg>
                                    </span>
                                    Course Name
                                </label>
                                <input type="text" id="course" name="course" placeholder="Enter course or program name" required>
                            </div>
                            <div class="form-group">
                                <label for="issueDate">
                                    <span class="label-icon">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                                            <line x1="16" y1="2" x2="16" y2="6"></line>
                                            <line x1="8" y1="2" x2="8" y2="6"></line>
                                            <line x1="3" y1="10" x2="21" y2="10"></line>
                                        </svg>
                                    </span>
                                    Issue Date
                                </label>
                                <input type="date" id="issueDate" name="issueDate" required>
                            </div>
                            <div class="form-group file-upload-group">
                                <label for="certificateFile">
                                    <span class="label-icon">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                            <polyline points="17 8 12 3 7 8"></polyline>
                                            <line x1="12" y1="3" x2="12" y2="15"></line>
                                        </svg>
                                    </span>
                                    Upload Certificate File <span class="optional">(Optional)</span>
                                </label>
                                <div class="file-upload-wrapper">
                                    <input type="file" id="certificateFile" name="certificateFile" accept=".pdf,.jpg,.jpeg,.png,.doc,.docx">
                                    <div class="file-upload-display">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                            <polyline points="17 8 12 3 7 8"></polyline>
                                            <line x1="12" y1="3" x2="12" y2="15"></line>
                                        </svg>
                                        <span class="file-text">Choose file or drag it here</span>
                                    </div>
                                </div>
                                <small class="form-hint">Upload a soft copy of the certificate. A cryptographic hash will be generated from the file content for verification.</small>
                            </div>
                            <button type="submit" class="btn btn-primary">
                                <span>Issue Certificate</span>
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                                    <path d="M10 3V17M3 10H17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                                </svg>
                            </button>
                        </form>
                        <div id="issueResult" class="result"></div>
                    </div>

                    <!-- Validate Certificate Tab -->
                    <div id="validate" class="tab-content">
                        <div class="tab-header">
                            <h2>Validate Certificate</h2>
                            <p class="tab-description">Verify the authenticity of a certificate using multiple validation methods</p>
                        </div>
                        <form id="validateForm" class="form" enctype="multipart/form-data">
                            <div class="form-group file-upload-group">
                                <label>
                                    <span class="label-icon">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <circle cx="11" cy="11" r="8"></circle>
                                            <path d="m21 21-4.35-4.35"></path>
                                        </svg>
                                    </span>
                                    Upload Certificate File
                                </label>
                                <div class="file-upload-wrapper">
                                    <input type="file" id="validateFile" name="certificateFile" accept=".pdf,.jpg,.jpeg,.png,.doc,.docx">
                                    <div class="file-upload-display">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                            <polyline points="17 8 12 3 7 8"></polyline>
                                            <line x1="12" y1="3" x2="12" y2="15"></line>
                                        </svg>
                                        <span class="file-text">Choose file or drag it here</span>
                                    </div>
                                </div>
                                <small class="form-hint">Upload the certificate file to validate by cryptographic file hash</small>
                            </div>
                            <div class="form-divider">
                                <span class="divider-line"></span>
                                <span class="divider-text">OR</span>
                                <span class="divider-line"></span>
                            </div>
                            <div class="form-group">
                                <label for="certificateId">
                                    <span class="label-icon">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                                        </svg>
                                    </span>
                                    Certificate ID
                                </label>
                                <input type="number" id="certificateId" name="certificateId" placeholder="Enter certificate ID">
                            </div>
                            <div class="form-group">
                                <label for="hash">
                                    <span class="label-icon">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                                            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                                        </svg>
                                    </span>
                                    Certificate Hash
                                </label>
                                <input type="text" id="hash" name="hash" placeholder="Enter 64-character SHA-256 hash">
                            </div>
                            <button type="submit" class="btn btn-primary">
                                <span>Validate Certificate</span>
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                                    <path d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" fill="currentColor"/>
                                </svg>
                            </button>
                        </form>
                        <div id="validateResult" class="result"></div>
                    </div>

                    <!-- View Certificates Tab -->
                    <div id="view" class="tab-content">
                        <div class="tab-header">
                            <h2>All Certificates</h2>
                            <p class="tab-description">Browse and manage all issued certificates</p>
                        </div>
                        <button onclick="loadCertificates()" class="btn btn-secondary">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polyline points="23 4 23 10 17 10"></polyline>
                                <polyline points="1 20 1 14 7 14"></polyline>
                                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
                            </svg>
                            <span>Refresh</span>
                        </button>
                        <div id="certificatesList" class="certificates-list"></div>
                    </div>

                    <!-- View Blockchain Tab -->
                    <div id="blockchain" class="tab-content">
                        <div class="tab-header">
                            <h2>Blockchain Explorer</h2>
                            <p class="tab-description">Explore the blockchain structure and verify integrity</p>
                        </div>
                        <button onclick="loadBlockchain()" class="btn btn-secondary">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polyline points="23 4 23 10 17 10"></polyline>
                                <polyline points="1 20 1 14 7 14"></polyline>
                                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
                            </svg>
                            <span>Refresh</span>
                        </button>
                        <div id="blockchainInfo" class="blockchain-info"></div>
                    </div>
                </main>
            </div>

            <script src="script.js"></script>
        </body>
        </html>
        """;
    }

    /**
     * Gets CSS content
     */
    private String getCssContent() {
        return """
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        :root {
            --primary: #667eea;
            --primary-dark: #5568d3;
            --secondary: #764ba2;
            --accent: #f093fb;
            --success: #10b981;
            --error: #ef4444;
            --warning: #f59e0b;
            --text-primary: #1f2937;
            --text-secondary: #6b7280;
            --bg-primary: #ffffff;
            --bg-secondary: #f9fafb;
            --border: #e5e7eb;
            --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
            --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
            --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
            --shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
        }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
            background-attachment: fixed;
            min-height: 100vh;
            color: var(--text-primary);
            line-height: 1.6;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
        }

        .container {
            max-width: 1400px;
            margin: 0 auto;
            padding: 40px 20px;
        }

        header {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(20px);
            border-radius: 24px;
            padding: 40px;
            margin-bottom: 32px;
            box-shadow: var(--shadow-xl);
            border: 1px solid rgba(255, 255, 255, 0.8);
        }

        .header-content {
            display: flex;
            align-items: center;
            gap: 24px;
            margin-bottom: 20px;
        }

        .logo {
            flex-shrink: 0;
        }

        .header-text h1 {
            font-size: 2.5rem;
            font-weight: 800;
            background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            margin-bottom: 8px;
            letter-spacing: -0.02em;
        }

        .header-text p {
            font-size: 1.1rem;
            color: var(--text-secondary);
            font-weight: 400;
        }

        .status-badge {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            background: linear-gradient(135deg, var(--success) 0%, #059669 100%);
            color: white;
            padding: 10px 20px;
            border-radius: 50px;
            font-size: 0.875rem;
            font-weight: 600;
            box-shadow: var(--shadow-md);
        }

        .badge-icon {
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .badge-icon svg {
            width: 16px;
            height: 16px;
        }

        .tabs {
            display: flex;
            justify-content: center;
            gap: 8px;
            margin-bottom: 32px;
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(20px);
            border-radius: 16px;
            padding: 8px;
            box-shadow: var(--shadow-lg);
            border: 1px solid rgba(255, 255, 255, 0.8);
        }

        .tab-button {
            display: flex;
            align-items: center;
            gap: 10px;
            background: none;
            border: none;
            padding: 14px 24px;
            cursor: pointer;
            border-radius: 12px;
            font-size: 0.95rem;
            font-weight: 600;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            color: var(--text-secondary);
            position: relative;
        }

        .tab-icon {
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .tab-icon svg {
            width: 20px;
            height: 20px;
        }

        .tab-button:hover {
            background: var(--bg-secondary);
            color: var(--text-primary);
            transform: translateY(-2px);
        }

        .tab-button.active {
            background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);
            color: white;
            box-shadow: var(--shadow-md);
            transform: translateY(-2px);
        }

        .tab-button.active .tab-icon svg {
            color: white;
        }

        main {
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(20px);
            border-radius: 24px;
            padding: 48px;
            box-shadow: var(--shadow-xl);
            border: 1px solid rgba(255, 255, 255, 0.8);
        }

        .tab-content {
            display: none;
            animation: fadeIn 0.4s ease-in-out;
        }

        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(10px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        .tab-content.active {
            display: block;
        }

        .tab-header {
            margin-bottom: 32px;
        }

        .tab-header h2 {
            font-size: 2rem;
            font-weight: 700;
            color: var(--text-primary);
            margin-bottom: 8px;
            letter-spacing: -0.02em;
        }

        .tab-description {
            font-size: 1rem;
            color: var(--text-secondary);
            font-weight: 400;
        }

        h2 {
            color: var(--text-primary);
            margin-bottom: 32px;
            font-size: 1.875rem;
            font-weight: 700;
            letter-spacing: -0.02em;
        }

        .form {
            max-width: 700px;
            margin: 0 auto;
        }

        .form-group {
            margin-bottom: 24px;
        }

        label {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 10px;
            font-weight: 600;
            color: var(--text-primary);
            font-size: 0.95rem;
        }

        .label-icon {
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .label-icon svg {
            width: 18px;
            height: 18px;
            color: var(--primary);
        }

        .optional {
            font-weight: 400;
            color: var(--text-secondary);
            font-size: 0.85em;
        }

        input[type="text"],
        input[type="number"],
        input[type="date"] {
            width: 100%;
            padding: 14px 16px;
            border: 2px solid var(--border);
            border-radius: 12px;
            font-size: 1rem;
            font-family: inherit;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            background: var(--bg-primary);
            color: var(--text-primary);
        }

        input[type="text"]:focus,
        input[type="number"]:focus,
        input[type="date"]:focus {
            outline: none;
            border-color: var(--primary);
            box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
            transform: translateY(-1px);
        }

        input::placeholder {
            color: var(--text-secondary);
            opacity: 0.6;
        }

        .file-upload-group {
            margin-bottom: 32px;
        }

        .file-upload-wrapper {
            position: relative;
        }

        .file-upload-wrapper input[type="file"] {
            position: absolute;
            width: 100%;
            height: 100%;
            opacity: 0;
            cursor: pointer;
            z-index: 2;
        }

        .file-upload-display {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 12px;
            padding: 48px 24px;
            border: 2px dashed var(--border);
            border-radius: 12px;
            background: var(--bg-secondary);
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            cursor: pointer;
        }

        .file-upload-wrapper:hover .file-upload-display {
            border-color: var(--primary);
            background: rgba(102, 126, 234, 0.05);
            transform: translateY(-2px);
        }

        .file-upload-display svg {
            color: var(--text-secondary);
            transition: color 0.3s;
        }

        .file-upload-wrapper:hover .file-upload-display svg {
            color: var(--primary);
        }

        .file-text {
            color: var(--text-secondary);
            font-weight: 500;
        }

        .form-hint {
            display: block;
            margin-top: 8px;
            font-size: 0.875rem;
            color: var(--text-secondary);
            line-height: 1.5;
        }

        .form-divider {
            display: flex;
            align-items: center;
            gap: 16px;
            margin: 32px 0;
        }

        .divider-line {
            flex: 1;
            height: 1px;
            background: linear-gradient(90deg, transparent, var(--border), transparent);
        }

        .divider-text {
            font-weight: 600;
            color: var(--text-secondary);
            font-size: 0.875rem;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        .btn {
            display: inline-flex;
            align-items: center;
            gap: 10px;
            padding: 14px 28px;
            border: none;
            border-radius: 12px;
            font-size: 1rem;
            font-weight: 600;
            font-family: inherit;
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            margin: 5px;
            position: relative;
            overflow: hidden;
        }

        .btn::before {
            content: '';
            position: absolute;
            top: 50%;
            left: 50%;
            width: 0;
            height: 0;
            border-radius: 50%;
            background: rgba(255, 255, 255, 0.2);
            transform: translate(-50%, -50%);
            transition: width 0.6s, height 0.6s;
        }

        .btn:hover::before {
            width: 300px;
            height: 300px;
        }

        .btn span,
        .btn svg {
            position: relative;
            z-index: 1;
        }

        .btn-primary {
            background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%);
            color: white;
            box-shadow: var(--shadow-md);
        }

        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: var(--shadow-lg);
        }

        .btn-primary:active {
            transform: translateY(0);
        }

        .btn-secondary {
            background: var(--bg-secondary);
            color: var(--text-primary);
            border: 2px solid var(--border);
        }

        .btn-secondary:hover {
            background: var(--border);
            transform: translateY(-2px);
        }

        .result {
            margin-top: 24px;
            padding: 20px 24px;
            border-radius: 12px;
            font-weight: 500;
            line-height: 1.6;
            animation: slideIn 0.3s ease-out;
        }

        @keyframes slideIn {
            from {
                opacity: 0;
                transform: translateY(-10px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }

        .result.success {
            background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
            color: #065f46;
            border: 2px solid var(--success);
            box-shadow: var(--shadow-sm);
        }

        .result.error {
            background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
            color: #991b1b;
            border: 2px solid var(--error);
            box-shadow: var(--shadow-sm);
        }

        .certificates-list {
            margin-top: 24px;
            display: grid;
            gap: 16px;
        }

        .certificate-item {
            background: var(--bg-primary);
            border: 2px solid var(--border);
            border-radius: 16px;
            padding: 24px;
            margin-bottom: 0;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: var(--shadow-sm);
            position: relative;
        }

        .certificate-item:hover {
            border-color: var(--primary);
            box-shadow: var(--shadow-md);
            transform: translateY(-2px);
        }

        .certificate-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 16px;
        }

        .certificate-item h3 {
            color: var(--primary);
            margin-bottom: 0;
            font-size: 1.25rem;
            font-weight: 700;
        }

        .btn-delete {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 36px;
            height: 36px;
            padding: 0;
            border: 2px solid var(--error);
            border-radius: 8px;
            background: transparent;
            color: var(--error);
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .btn-delete:hover {
            background: var(--error);
            color: white;
            transform: scale(1.1);
        }

        .btn-delete:active {
            transform: scale(0.95);
        }

        .btn-delete svg {
            width: 18px;
            height: 18px;
        }

        .certificate-actions {
            display: flex;
            gap: 8px;
            align-items: center;
        }

        .btn-download {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 36px;
            height: 36px;
            padding: 0;
            border: 2px solid var(--primary);
            border-radius: 8px;
            background: transparent;
            color: var(--primary);
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }

        .btn-download:hover {
            background: var(--primary);
            color: white;
            transform: scale(1.1);
        }

        .btn-download:active {
            transform: scale(0.95);
        }

        .btn-download svg {
            width: 18px;
            height: 18px;
        }

        .certificate-item p {
            margin: 8px 0;
            color: var(--text-secondary);
            font-size: 0.95rem;
        }

        .certificate-item strong {
            color: var(--text-primary);
            font-weight: 600;
        }

        .blockchain-info {
            margin-top: 24px;
        }

        .block-item {
            background: var(--bg-primary);
            border: 2px solid var(--border);
            border-radius: 16px;
            padding: 24px;
            margin-bottom: 16px;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            box-shadow: var(--shadow-sm);
        }

        .block-item:hover {
            border-color: var(--primary);
            box-shadow: var(--shadow-md);
            transform: translateY(-2px);
        }

        .block-item h3 {
            color: var(--primary);
            margin-bottom: 16px;
            font-size: 1.25rem;
            font-weight: 700;
        }

        .block-item p {
            margin: 10px 0;
            color: var(--text-secondary);
            font-family: 'Courier New', monospace;
            font-size: 0.875rem;
            word-break: break-all;
        }

        .block-item strong {
            color: var(--text-primary);
            font-weight: 600;
            font-family: inherit;
        }

        .status-valid {
            color: var(--success);
            font-weight: 700;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }
        
        .status-valid svg {
            width: 16px;
            height: 16px;
        }

        .status-invalid {
            color: var(--error);
            font-weight: 700;
            display: inline-flex;
            align-items: center;
            gap: 6px;
        }
        
        .status-invalid svg {
            width: 16px;
            height: 16px;
        }
        
        code {
            font-family: 'Courier New', monospace;
            background: rgba(0, 0, 0, 0.05);
            padding: 2px 6px;
            border-radius: 4px;
            font-size: 0.9em;
        }

        @media (max-width: 768px) {
            .container {
                padding: 10px;
            }
            
            header h1 {
                font-size: 2rem;
            }
            
            .tabs {
                flex-direction: column;
            }
            
            .tab-button {
                margin: 2px 0;
            }
            
            main {
                padding: 20px;
            }
        }
        """;
    }

    /**
     * Gets JavaScript content
     */
    private String getJavaScriptContent() {
        return """
        // Set today's date as default
        document.addEventListener('DOMContentLoaded', function() {
            const today = new Date().toISOString().split('T')[0];
            document.getElementById('issueDate').value = today;
            
            // File upload display handlers
            setupFileUploads();
        });
        
        function setupFileUploads() {
            const fileInputs = document.querySelectorAll('input[type="file"]');
            fileInputs.forEach(input => {
                input.addEventListener('change', function(e) {
                    const wrapper = this.closest('.file-upload-wrapper');
                    if (wrapper) {
                        const display = wrapper.querySelector('.file-upload-display');
                        const fileText = display.querySelector('.file-text');
                        
                        if (this.files && this.files.length > 0) {
                            fileText.textContent = this.files[0].name;
                            display.style.borderColor = 'var(--primary)';
                            display.style.background = 'rgba(102, 126, 234, 0.05)';
                        } else {
                            fileText.textContent = 'Choose file or drag it here';
                            display.style.borderColor = '';
                            display.style.background = '';
                        }
                    }
                });
            });
        }

        function showTab(tabName) {
            // Hide all tab contents
            const tabContents = document.querySelectorAll('.tab-content');
            tabContents.forEach(content => content.classList.remove('active'));

            // Remove active class from all tab buttons
            const tabButtons = document.querySelectorAll('.tab-button');
            tabButtons.forEach(button => button.classList.remove('active'));

            // Show selected tab content
            document.getElementById(tabName).classList.add('active');

            // Add active class to clicked button
            event.target.classList.add('active');

            // Auto-load data for view tabs
            if (tabName === 'view') {
                loadCertificates();
            } else if (tabName === 'blockchain') {
                loadBlockchain();
            }
        }

        // Issue Certificate Form
        document.getElementById('issueForm').addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const formData = new FormData(this);
            formData.append('action', 'issue');

            try {
                const response = await fetch('/', {
                    method: 'POST',
                    body: formData
                });

                const result = await response.json();
                const resultDiv = document.getElementById('issueResult');
                
                if (result.success) {
                    let message = `
                        <div class="result success">
                            <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                                    <polyline points="20 6 9 17 4 12"></polyline>
                                </svg>
                                <strong>Certificate issued successfully!</strong>
                            </div>
                            <div style="margin-left: 28px;">
                                <p>Certificate ID: <strong>${result.certificateId}</strong></p>
                                <p>Hash: <code style="font-size: 0.85em; word-break: break-all;">${result.hash}</code></p>
                                <p>Block Index: <strong>${result.blockIndex}</strong></p>
                    `;
                    if (result.fileHash) {
                        message += `<p>File Hash: <code style="font-size: 0.85em; word-break: break-all;">${result.fileHash}</code></p>`;
                    }
                    message += `</div></div>`;
                    resultDiv.innerHTML = message;
                    this.reset();
                    document.getElementById('issueDate').value = new Date().toISOString().split('T')[0];
                } else {
                    resultDiv.innerHTML = `<div class="result error">
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <strong>Error:</strong> ${result.error}
                        </div>
                    </div>`;
                }
            } catch (error) {
                document.getElementById('issueResult').innerHTML = 
                    `<div class="result error">
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <strong>Error:</strong> ${error.message}
                        </div>
                    </div>`;
            }
        });

        // Validate Certificate Form
        document.getElementById('validateForm').addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const formData = new FormData(this);
            formData.append('action', 'validate');
            
            // Only append text fields if file is not selected
            const fileInput = document.getElementById('validateFile');
            if (!fileInput.files || fileInput.files.length === 0) {
                if (formData.get('certificateId')) {
                    // Keep it in FormData
                }
                if (formData.get('hash')) {
                    // Keep it in FormData
                }
            }

            try {
                const response = await fetch('/', {
                    method: 'POST',
                    body: formData
                });

                const result = await response.json();
                const resultDiv = document.getElementById('validateResult');
                
                if (result.success) {
                    const statusClass = result.valid ? 'success' : 'error';
                    const statusText = result.valid ? 'AUTHENTIC' : 'INVALID';
                    const statusIcon = result.valid ? 
                        '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"></polyline></svg>' :
                        '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>';
                    
                    let message = `
                        <div class="result ${statusClass}">
                            <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px;">
                                ${statusIcon}
                                <strong>Certificate is ${statusText}</strong>
                            </div>
                            <div style="margin-left: 28px;">
                                <p>${result.message}</p>
                    `;
                    if (result.hash) {
                        message += `<p>Certificate Hash: <code style="font-size: 0.85em; word-break: break-all;">${result.hash}</code></p>`;
                    }
                    message += `</div></div>`;
                    resultDiv.innerHTML = message;
                } else {
                    resultDiv.innerHTML = `<div class="result error">
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <strong>Error:</strong> ${result.error}
                        </div>
                    </div>`;
                }
            } catch (error) {
                document.getElementById('validateResult').innerHTML = 
                    `<div class="result error">
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <strong>Error:</strong> ${error.message}
                        </div>
                    </div>`;
            }
        });

        // Load Certificates
        async function loadCertificates() {
            try {
                const response = await fetch('/api/certificates');
                const certificates = await response.json();
                
                const container = document.getElementById('certificatesList');
                
                if (certificates.length === 0) {
                    container.innerHTML = '<p style="text-align: center; color: var(--text-secondary); padding: 40px;">No certificates found.</p>';
                    return;
                }
                
                container.innerHTML = certificates.map(cert => {
                    const safeName = cert.studentName.replace(/'/g, "\\'").replace(/"/g, '&quot;');
                    const hasFile = cert.filePath && cert.filePath !== null && cert.filePath !== '';
                    return `
                    <div class="certificate-item">
                        <div class="certificate-header">
                            <h3>Certificate #${cert.id}</h3>
                            <div class="certificate-actions">
                                ${hasFile ? `
                                <button class="btn-download" onclick="downloadCertificate(${cert.id})" title="Download certificate file">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                        <polyline points="7 10 12 15 17 10"></polyline>
                                        <line x1="12" y1="15" x2="12" y2="3"></line>
                                    </svg>
                                </button>
                                ` : ''}
                                <button class="btn-delete" onclick="deleteCertificate(${cert.id}, '${safeName}')" title="Delete certificate">
                                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                        <polyline points="3 6 5 6 21 6"></polyline>
                                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                        <line x1="10" y1="11" x2="10" y2="17"></line>
                                        <line x1="14" y1="11" x2="14" y2="17"></line>
                                    </svg>
                                </button>
                            </div>
                        </div>
                        <p><strong>Student:</strong> ${cert.studentName}</p>
                        <p><strong>Course:</strong> ${cert.course}</p>
                        <p><strong>Issue Date:</strong> ${cert.issueDate}</p>
                        <p><strong>Hash:</strong> <code style="font-size: 0.85em; word-break: break-all;">${cert.hash}</code></p>
                    </div>
                `;
                }).join('');
            } catch (error) {
                document.getElementById('certificatesList').innerHTML = 
                    `<div class="result error">
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <strong>Error loading certificates:</strong> ${error.message}
                        </div>
                    </div>`;
            }
        }

        // Download Certificate
        function downloadCertificate(certificateId) {
            window.location.href = `/api/certificates/download/${certificateId}`;
        }

        // Delete Certificate
        async function deleteCertificate(certificateId, studentName) {
            if (!confirm(`Are you sure you want to delete Certificate #${certificateId} for ${studentName}?\\n\\nThis action cannot be undone.`)) {
                return;
            }
            
            try {
                const response = await fetch(`/api/certificates/delete/${certificateId}`, {
                    method: 'DELETE'
                });
                
                const result = await response.json();
                
                if (result.success) {
                    // Show success message
                    const container = document.getElementById('certificatesList');
                    container.innerHTML = `
                        <div class="result success" style="margin-bottom: 16px;">
                            <div style="display: flex; align-items: center; gap: 8px;">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                                    <polyline points="20 6 9 17 4 12"></polyline>
                                </svg>
                                <strong>Certificate deleted successfully!</strong>
                            </div>
                        </div>
                    `;
                    
                    // Reload certificates after a short delay
                    setTimeout(() => {
                        loadCertificates();
                    }, 1000);
                } else {
                    alert('Error: ' + (result.error || 'Failed to delete certificate'));
                }
            } catch (error) {
                alert('Error deleting certificate: ' + error.message);
            }
        }

        // Load Blockchain
        async function loadBlockchain() {
            try {
                const response = await fetch('/api/blockchain');
                const blockchain = await response.json();
                
                const container = document.getElementById('blockchainInfo');
                
                const validIcon = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"></polyline></svg>';
                const invalidIcon = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>';
                
                container.innerHTML = `
                    <div class="blockchain-info">
                        <h3>Blockchain Status</h3>
                        <p><strong>Total Blocks:</strong> ${blockchain.chainLength}</p>
                        <p><strong>Chain Valid:</strong> <span class="${blockchain.valid ? 'status-valid' : 'status-invalid'}">
                            ${blockchain.valid ? validIcon + ' Valid' : invalidIcon + ' Invalid'}
                        </span></p>
                    </div>
                    ${blockchain.blocks.map(block => `
                        <div class="block-item">
                            <h3>Block ${block.index}</h3>
                            <p><strong>Timestamp:</strong> ${block.timestamp}</p>
                            <p><strong>Certificate Hash:</strong> <code>${block.certificateHash}</code></p>
                            <p><strong>Previous Hash:</strong> <code>${block.previousHash}</code></p>
                            <p><strong>Current Hash:</strong> <code>${block.currentHash}</code></p>
                            <p><strong>Valid:</strong> <span class="${block.valid ? 'status-valid' : 'status-invalid'}">
                                ${block.valid ? validIcon : invalidIcon}
                            </span></p>
                        </div>
                    `).join('')}
                `;
            } catch (error) {
                document.getElementById('blockchainInfo').innerHTML = 
                    `<div class="result error">
                        <div style="display: flex; align-items: center; gap: 8px;">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="12" y1="8" x2="12" y2="12"></line>
                                <line x1="12" y1="16" x2="12.01" y2="16"></line>
                            </svg>
                            <strong>Error loading blockchain:</strong> ${error.message}
                        </div>
                    </div>`;
            }
        }
        """;
    }

    /**
     * Gets certificates JSON
     */
    private String getCertificatesJson() {
        try {
            List<Certificate> certs = certificateDAO.findAll();
            StringBuilder json = new StringBuilder("[");
            
            for (int i = 0; i < certs.size(); i++) {
                Certificate cert = certs.get(i);
                String filePath = certificateDAO.getFilePath(cert.getId());
                if (i > 0) json.append(",");
                json.append("{")
                    .append("\"id\":").append(cert.getId()).append(",")
                    .append("\"studentName\":\"").append(escapeJson(cert.getStudentName())).append("\",")
                    .append("\"course\":\"").append(escapeJson(cert.getCourse())).append("\",")
                    .append("\"issueDate\":\"").append(cert.getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\",")
                    .append("\"hash\":\"").append(cert.getHash()).append("\",")
                    .append("\"filePath\":").append(filePath != null ? "\"" + escapeJson(filePath) + "\"" : "null")
                    .append("}");
            }
            
            json.append("]");
            return json.toString();
        } catch (SQLException e) {
            return "[]";
        }
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Gets blockchain JSON
     */
    private String getBlockchainJson() {
        var blocks = blockchain.getChain();
        
        StringBuilder json = new StringBuilder("{");
        json.append("\"chainLength\":").append(blockchain.getChainLength()).append(",");
        json.append("\"valid\":").append(blockchain.isChainValid()).append(",");
        json.append("\"blocks\":[");
        
        for (int i = 0; i < blocks.size(); i++) {
            var block = blocks.get(i);
            if (i > 0) json.append(",");
            json.append("{")
                .append("\"index\":").append(block.getIndex()).append(",")
                .append("\"timestamp\":\"").append(block.getTimestamp()).append("\",")
                .append("\"certificateHash\":\"").append(block.getCertificateHash()).append("\",")
                .append("\"previousHash\":\"").append(block.getPreviousHash()).append("\",")
                .append("\"currentHash\":\"").append(block.getCurrentHash()).append("\",")
                .append("\"valid\":").append(block.isValid())
                .append("}");
        }
        
        json.append("]}");
        return json.toString();
    }
}
