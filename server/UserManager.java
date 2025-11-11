package server;

import java.util.*;

public class UserManager {
    private static Map<String, String> registeredUsers = new HashMap<>();
    
    // Static block to pre-register 20 students
    static {
        registeredUsers.put("student1", "pass123");
        registeredUsers.put("student2", "pass123");
        registeredUsers.put("student3", "pass123");
        registeredUsers.put("student4", "pass123");
        registeredUsers.put("student5", "pass123");
        registeredUsers.put("student6", "pass123");
        registeredUsers.put("student7", "pass123");
        registeredUsers.put("student8", "pass123");
        registeredUsers.put("student9", "pass123");
        registeredUsers.put("student10", "pass123");
        registeredUsers.put("student11", "pass123");
        registeredUsers.put("student12", "pass123");
        registeredUsers.put("student13", "pass123");
        registeredUsers.put("student14", "pass123");
        registeredUsers.put("student15", "pass123");
        registeredUsers.put("student16", "pass123");
        registeredUsers.put("student17", "pass123");
        registeredUsers.put("student18", "pass123");
        registeredUsers.put("student19", "pass123");
        registeredUsers.put("student20", "pass123");
        
        System.out.println("✅ 20 students registered in the system");
    }
    
    /**
     * Authenticate a user with username and password
     * @param username The username to check
     * @param password The password to verify
     * @return true if credentials are valid, false otherwise
     */
    public static boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        
        String storedPassword = registeredUsers.get(username);
        if (storedPassword == null) {
            System.out.println("❌ Login attempt failed: User '" + username + "' not registered");
            return false;
        }
        
        if (storedPassword.equals(password)) {
            System.out.println("✅ Login successful: " + username);
            return true;
        } else {
            System.out.println("❌ Login attempt failed: Invalid password for '" + username + "'");
            return false;
        }
    }
    
    /**
     * Register a new user (optional - for future use)
     * @param username The username to register
     * @param password The password for the user
     * @return true if registration successful, false if user already exists
     */
    public static boolean registerUser(String username, String password) {
        if (registeredUsers.containsKey(username)) {
            return false;
        }
        registeredUsers.put(username, password);
        return true;
    }
    
    /**
     * Check if a user is registered
     * @param username The username to check
     * @return true if user exists, false otherwise
     */
    public static boolean isUserRegistered(String username) {
        return registeredUsers.containsKey(username);
    }
    
    /**
     * Get total number of registered users
     * @return count of registered users
     */
    public static int getRegisteredUserCount() {
        return registeredUsers.size();
    }
}
