package server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Simple HTTP REST Server using Socket Programming
 * Provides REST API endpoints for exam management
 * 
 * Endpoints:
 * GET  /api/status        - Get exam status
 * POST /api/exam/start    - Start the exam
 * POST /api/exam/stop     - Stop the exam
 * GET  /api/students      - Get connected students count
 * GET  /api/results       - Get exam results
 */
public class HttpRestServer {
    private static final int HTTP_PORT = 8080;
    private static volatile boolean running = true;
    private static ExecutorService pool = Executors.newFixedThreadPool(5);
    
    public static void main(String[] args) {
        System.out.println("🌐 HTTP REST Server starting on port " + HTTP_PORT);
        
        try (ServerSocket serverSocket = new ServerSocket(HTTP_PORT)) {
            System.out.println("✅ HTTP REST Server started on http://localhost:" + HTTP_PORT);
            System.out.println("\n📋 Available endpoints:");
            System.out.println("   GET  http://localhost:8080/api/status");
            System.out.println("   POST http://localhost:8080/api/exam/start");
            System.out.println("   POST http://localhost:8080/api/exam/stop");
            System.out.println("   GET  http://localhost:8080/api/students");
            System.out.println("   GET  http://localhost:8080/api/results\n");
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new HttpRequestHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static class HttpRequestHandler implements Runnable {
        private Socket socket;
        
        public HttpRequestHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream())) {
                
                // Read HTTP request
                String requestLine = in.readLine();
                if (requestLine == null) return;
                
                System.out.println("📥 HTTP Request: " + requestLine);
                
                // Read headers
                Map<String, String> headers = new HashMap<>();
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    int colonIndex = line.indexOf(':');
                    if (colonIndex > 0) {
                        String key = line.substring(0, colonIndex).trim();
                        String value = line.substring(colonIndex + 1).trim();
                        headers.put(key, value);
                    }
                }
                
                // Read body if present
                StringBuilder body = new StringBuilder();
                if (headers.containsKey("Content-Length")) {
                    int contentLength = Integer.parseInt(headers.get("Content-Length"));
                    char[] buffer = new char[contentLength];
                    in.read(buffer, 0, contentLength);
                    body.append(buffer);
                }
                
                // Parse request
                String[] requestParts = requestLine.split(" ");
                String method = requestParts[0];
                String path = requestParts[1];
                
                // Handle CORS preflight (OPTIONS)
                if ("OPTIONS".equals(method)) {
                    sendCORSResponse(out);
                    return;
                }
                
                // Route request
                String response = handleRequest(method, path, body.toString());
                
                // Determine content type
                String contentType = "application/json";
                if (path.equals("/") || path.equals("/dashboard")) {
                    contentType = "text/html";
                }
                
                // Send HTTP response
                out.print("HTTP/1.1 200 OK\r\n");
                out.print("Content-Type: " + contentType + "\r\n");
                out.print("Access-Control-Allow-Origin: *\r\n");
                out.print("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n");
                out.print("Access-Control-Allow-Headers: Content-Type\r\n");
                out.print("Content-Length: " + response.getBytes(StandardCharsets.UTF_8).length + "\r\n");
                out.print("\r\n");
                out.print(response);
                out.flush();
                
                System.out.println("📤 Response sent: " + response.substring(0, Math.min(100, response.length())) + "...");
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private String serveDashboard() {
            try {
                File dashboardFile = new File("dashboard.html");
                if (dashboardFile.exists()) {
                    StringBuilder content = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new FileReader(dashboardFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            content.append(line).append("\n");
                        }
                    }
                    return content.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "<html><body><h1>Dashboard not found</h1><p>Please ensure dashboard.html exists in the project directory.</p></body></html>";
        }
        
        private void sendCORSResponse(PrintWriter out) {
            out.print("HTTP/1.1 200 OK\r\n");
            out.print("Access-Control-Allow-Origin: *\r\n");
            out.print("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n");
            out.print("Access-Control-Allow-Headers: Content-Type\r\n");
            out.print("Content-Length: 0\r\n");
            out.print("\r\n");
            out.flush();
        }
        
        private String handleRequest(String method, String path, String body) {
            // Serve dashboard HTML
            if (path.equals("/") || path.equals("/dashboard")) {
                return serveDashboard();
            }
            
            // Ignore favicon requests
            if (path.equals("/favicon.ico")) {
                return "{\"error\": \"Not found\"}";
            }
            
            switch (path) {
                case "/api/status":
                    return handleGetStatus();
                    
                case "/api/exam/start":
                    if ("POST".equals(method)) {
                        return handleStartExam();
                    }
                    break;
                    
                case "/api/exam/stop":
                    if ("POST".equals(method)) {
                        return handleStopExam();
                    }
                    break;
                    
                case "/api/students":
                    return handleGetStudents();
                    
                case "/api/results":
                    return handleGetResults();
                    
                default:
                    return "{\"error\": \"Endpoint not found\", \"path\": \"" + path + "\"}";
            }
            
            return "{\"error\": \"Method not allowed\", \"method\": \"" + method + "\"}";
        }
        
        private String handleGetStatus() {
            boolean examStarted = ExamServer.isExamStarted();
            int waitingCount = ExamServer.getWaitingClientsCount();
            
            return String.format(
                "{\"status\": \"success\", \"examStarted\": %b, \"waitingStudents\": %d, \"timestamp\": %d}",
                examStarted, waitingCount, System.currentTimeMillis()
            );
        }
        
        private String handleStartExam() {
            try {
                if (ExamServer.isExamStarted()) {
                    return "{\"status\": \"error\", \"message\": \"Exam already started\"}";
                }
                
                ExamServer.startExamViaAPI();
                return "{\"status\": \"success\", \"message\": \"Exam started successfully\"}";
            } catch (Exception e) {
                return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
            }
        }
        
        private String handleStopExam() {
            return "{\"status\": \"success\", \"message\": \"Exam stop functionality not implemented\"}";
        }
        
        private String handleGetStudents() {
            int waitingCount = ExamServer.getWaitingClientsCount();
            return String.format(
                "{\"status\": \"success\", \"totalStudents\": 20, \"connectedStudents\": %d}",
                waitingCount
            );
        }
        
        private String handleGetResults() {
            // This would return actual results from ResultManager
            return "{\"status\": \"success\", \"results\": [], \"message\": \"Results endpoint active\"}";
        }
    }
}