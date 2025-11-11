package broadcaster;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

/**
 * TimerBroadcaster using Java NIO (DatagramChannel + ByteBuffer)
 * Demonstrates non-blocking I/O for UDP broadcasting
 */
public class TimerBroadcaster {
    private static final int UDP_PORT = 9876;
    private static final int CONTROL_PORT = 9877; // Port to receive START command
    private static volatile boolean examStarted = false;
    
    public static void main(String[] args) {
        System.out.println("⏳ TimerBroadcaster initialized (Using Java NIO). Waiting for exam to start...");
        
        // Thread to listen for START signal using NIO DatagramChannel
        new Thread(() -> {
            try (DatagramChannel controlChannel = DatagramChannel.open()) {
                controlChannel.socket().bind(new InetSocketAddress(CONTROL_PORT));
                controlChannel.configureBlocking(false); // Non-blocking mode
                
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                
                System.out.println("📡 NIO DatagramChannel listening on port " + CONTROL_PORT + " (non-blocking)");
                
                while (!examStarted) {
                    buffer.clear();
                    SocketAddress sender = controlChannel.receive(buffer);
                    
                    if (sender != null) {
                        buffer.flip();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);
                        String message = new String(data, StandardCharsets.UTF_8);
                        
                        if (message.equals("START_EXAM")) {
                            examStarted = true;
                            System.out.println("🚀 START signal received via NIO! Beginning countdown...\n");
                            break;
                        }
                    }
                    
                    // Small sleep to prevent busy waiting
                    Thread.sleep(100);
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
        
        // Start countdown using NIO DatagramChannel
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.socket().setBroadcast(true); // Enable broadcast
            InetSocketAddress broadcastAddress = new InetSocketAddress("255.255.255.255", UDP_PORT);
            
            System.out.println("📡 Broadcasting via NIO DatagramChannel on port " + UDP_PORT);
            
            int countdown = 60; // 60 seconds exam duration
            while (countdown >= 0) {
                String msg = "Time left: " + countdown + " sec";
                
                // Use ByteBuffer (NIO way)
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8));
                channel.send(buffer, broadcastAddress);

                System.out.println("Broadcasting (NIO): " + msg);

                Thread.sleep(1000);
                countdown--;
            }
            
            // Send EXAM_FINISHED signal
            String finishMsg = "EXAM_FINISHED";
            ByteBuffer finishBuffer = ByteBuffer.wrap(finishMsg.getBytes(StandardCharsets.UTF_8));
            channel.send(finishBuffer, broadcastAddress);
            
            System.out.println("\n⏰ Countdown finished! Exam time ended.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
