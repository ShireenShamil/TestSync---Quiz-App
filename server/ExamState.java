package server;

import java.io.*;

/**
 * Shared state holder for exam status that both ExamServer and AdminHTTPServer can access.
 * Uses a shared file to persist state across multiple JVM processes.
 */
public class ExamState {
    private static volatile String state = "WAITING"; // WAITING, RUNNING, FINISHED
    private static volatile int connectedStudents = 0;
    private static volatile long examStartTime = -1;
    private static final int EXAM_DURATION_SECONDS = 60;
    private static final String STATE_FILE = "exam_state.txt";
    
    public static synchronized void setState(String newState) {
        state = newState;
        if ("RUNNING".equals(newState)) {
            examStartTime = System.currentTimeMillis();
        }
        saveState();
    }
    
    public static String getState() {
        loadState();
        return state;
    }
    
    public static synchronized void incrementConnectedStudents() {
        connectedStudents++;
        saveState();
    }
    
    public static synchronized void decrementConnectedStudents() {
        if (connectedStudents > 0) {
            connectedStudents--;
        }
        saveState();
    }
    
    public static int getConnectedStudents() {
        loadState();
        System.out.println("[ExamState] getConnectedStudents() called - returning: " + connectedStudents);
        return connectedStudents;
    }
    
    public static long getExamStartTime() {
        return examStartTime;
    }
    
    public static int getExamDurationSeconds() {
        return EXAM_DURATION_SECONDS;
    }
    
    private static synchronized void saveState() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(STATE_FILE))) {
            writer.println(state);
            writer.println(connectedStudents);
            writer.println(examStartTime);
            System.out.println("[ExamState] Saved state: " + state + ", connected: " + connectedStudents);
        } catch (IOException e) {
            System.err.println("Error saving exam state: " + e.getMessage());
        }
    }
    
    private static synchronized void loadState() {
        try (BufferedReader reader = new BufferedReader(new FileReader(STATE_FILE))) {
            String line = reader.readLine();
            if (line != null) state = line;
            
            line = reader.readLine();
            if (line != null) {
                try { connectedStudents = Integer.parseInt(line); } catch (NumberFormatException ignored) {}
            }
            
            line = reader.readLine();
            if (line != null) {
                try { examStartTime = Long.parseLong(line); } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            // File doesn't exist yet, that's fine
        }
    }
    
    public static String getExamProgressJson() {
        loadState();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"state\": \"").append(state).append("\",\n");
        sb.append("  \"connectedStudents\": ").append(connectedStudents).append(",\n");
        sb.append("  \"examStartTime\": ").append(examStartTime).append(",\n");
        sb.append("  \"examDurationSeconds\": ").append(EXAM_DURATION_SECONDS).append(",\n");
        sb.append("  \"totalRegistered\": ").append(UserManager.getRegisteredUserCount()).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
