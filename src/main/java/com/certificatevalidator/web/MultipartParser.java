package com.certificatevalidator.web;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses multipart/form-data requests for file uploads.
 */
public class MultipartParser {
    private final String boundary;
    private final byte[] requestBody;
    
    public MultipartParser(String contentType, byte[] requestBody) {
        this.requestBody = requestBody;
        // Extract boundary from Content-Type header
        if (contentType != null && contentType.contains("boundary=")) {
            this.boundary = "--" + contentType.substring(contentType.indexOf("boundary=") + 9);
        } else {
            this.boundary = null;
        }
    }
    
    /**
     * Parses multipart form data and extracts fields and files
     */
    public MultipartData parse() throws IOException {
        if (boundary == null) {
            throw new IOException("No boundary found in Content-Type");
        }
        
        MultipartData data = new MultipartData();
        byte[] boundaryBytes = boundary.getBytes(StandardCharsets.UTF_8);
        
        int start = findBoundary(0, boundaryBytes);
        if (start == -1) {
            return data;
        }
        
        while (start < requestBody.length) {
            int end = findBoundary(start + boundaryBytes.length, boundaryBytes);
            if (end == -1) {
                end = requestBody.length;
            }
            
            // Extract part between boundaries
            byte[] part = new byte[end - start - boundaryBytes.length - 2]; // -2 for \r\n
            System.arraycopy(requestBody, start + boundaryBytes.length + 2, part, 0, part.length);
            
            // Parse the part
            parsePart(part, data);
            
            if (end >= requestBody.length) {
                break;
            }
            start = end;
        }
        
        return data;
    }
    
    private int findBoundary(int start, byte[] boundaryBytes) {
        for (int i = start; i <= requestBody.length - boundaryBytes.length; i++) {
            boolean match = true;
            for (int j = 0; j < boundaryBytes.length; j++) {
                if (requestBody[i + j] != boundaryBytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }
    
    private void parsePart(byte[] part, MultipartData data) throws IOException {
        // Find the header/body separator (empty line)
        int headerEnd = -1;
        for (int i = 0; i < part.length - 3; i++) {
            if (part[i] == '\r' && part[i + 1] == '\n' && 
                part[i + 2] == '\r' && part[i + 3] == '\n') {
                headerEnd = i;
                break;
            }
        }
        
        if (headerEnd == -1) {
            return;
        }
        
        // Parse headers
        String headers = new String(part, 0, headerEnd, StandardCharsets.UTF_8);
        String name = extractName(headers);
        String filename = extractFilename(headers);
        
        // Extract body (skip the \r\n after headers)
        int bodyStart = headerEnd + 4;
        // Remove trailing \r\n
        int bodyEnd = part.length;
        if (bodyEnd >= 2 && part[bodyEnd - 2] == '\r' && part[bodyEnd - 1] == '\n') {
            bodyEnd -= 2;
        }
        
        byte[] body = new byte[bodyEnd - bodyStart];
        System.arraycopy(part, bodyStart, body, 0, body.length);
        
        if (filename != null && !filename.isEmpty()) {
            // It's a file
            data.addFile(name, filename, body);
        } else {
            // It's a regular field
            String value = new String(body, StandardCharsets.UTF_8);
            data.addField(name, value);
        }
    }
    
    private String extractName(String headers) {
        int nameStart = headers.indexOf("name=\"");
        if (nameStart == -1) return null;
        nameStart += 6;
        int nameEnd = headers.indexOf("\"", nameStart);
        if (nameEnd == -1) return null;
        return headers.substring(nameStart, nameEnd);
    }
    
    private String extractFilename(String headers) {
        int filenameStart = headers.indexOf("filename=\"");
        if (filenameStart == -1) return null;
        filenameStart += 10;
        int filenameEnd = headers.indexOf("\"", filenameStart);
        if (filenameEnd == -1) return null;
        return headers.substring(filenameStart, filenameEnd);
    }
    
    /**
     * Container for parsed multipart data
     */
    public static class MultipartData {
        private final Map<String, String> fields = new HashMap<>();
        private final Map<String, FileData> files = new HashMap<>();
        
        public void addField(String name, String value) {
            if (name != null) {
                fields.put(name, value);
            }
        }
        
        public void addFile(String name, String filename, byte[] data) {
            if (name != null) {
                files.put(name, new FileData(filename, data));
            }
        }
        
        public String getField(String name) {
            return fields.get(name);
        }
        
        public FileData getFile(String name) {
            return files.get(name);
        }
        
        public boolean hasFile(String name) {
            return files.containsKey(name);
        }
        
        public static class FileData {
            private final String filename;
            private final byte[] data;
            
            public FileData(String filename, byte[] data) {
                this.filename = filename;
                this.data = data;
            }
            
            public String getFilename() {
                return filename;
            }
            
            public byte[] getData() {
                return data;
            }
        }
    }
}

