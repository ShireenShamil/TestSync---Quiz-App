package broadcaster;

import java.net.*;
import java.io.*;

public class TimerBroadcaster {
    private static final int UDP_PORT = 9876;
    private static final int CONTROL_PORT = 9877; // Port to receive START command
    private static volatile boolean examStarted = false;
    
    public static void main(String[] args) {
        System.out.println("⏳ TimerBroadcaster initialized. Waiting for exam to start...");
        
        // Thread to listen for START signal
        new Thread(() -> {
            try (DatagramSocket controlSocket = new DatagramSocket(CONTROL_PORT)) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                while (!examStarted) {
                    controlSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (message.equals("START_EXAM")) {
                        examStarted = true;
                        System.out.println("🚀 START signal received! Beginning countdown...\n");
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        
        // Wait until exam starts
        while (!examStarted) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // Start countdown - use multicast instead of broadcast for better reliability
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            socket.setReuseAddress(true);
            int countdown = 60; // 60 seconds exam duration
            while (countdown >= 0) {
                String msg = "Time left: " + countdown + " sec";
                byte[] buffer = msg.getBytes();
                
                // Send to broadcast address - will reach all clients on the network
                try {
                    InetAddress broadcast = InetAddress.getByName("255.255.255.255");
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcast, UDP_PORT);
                    socket.send(packet);
                    System.out.println("Broadcasting: " + msg);
                } catch (Exception e) {
                    System.err.println("Failed to broadcast: " + e.getMessage());
                }

                Thread.sleep(1000);
                countdown--;
            }
            
            // Send EXAM_FINISHED signal
            String finishMsg = "EXAM_FINISHED";
            byte[] finishBuffer = finishMsg.getBytes();
            try {
                InetAddress broadcast = InetAddress.getByName("255.255.255.255");
                DatagramPacket finishPacket = new DatagramPacket(finishBuffer, finishBuffer.length, broadcast, UDP_PORT);
                socket.send(finishPacket);
                System.out.println("Broadcasting: EXAM_FINISHED");
            } catch (Exception e) {}
            
            System.out.println("\n⏰ Countdown finished! Exam time ended.");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
