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
        
        // Start countdown using MulticastSocket so multiple clients can receive updates
        try (MulticastSocket mcastSocket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName("230.0.0.0");
            mcastSocket.setTimeToLive(1); // Keep multicast local

            System.out.println("📡 Broadcasting via MulticastSocket on group 230.0.0.0:" + UDP_PORT);

            int countdown = 60; // 60 seconds exam duration
            while (countdown >= 0) {
                String msg = "Time left: " + countdown + " sec";
                byte[] buf = msg.getBytes(StandardCharsets.UTF_8);
                DatagramPacket packet = new DatagramPacket(buf, buf.length, group, UDP_PORT);
                mcastSocket.send(packet);

                System.out.println("Broadcasting: " + msg);

                Thread.sleep(1000);
                countdown--;
            }

            // Send EXAM_FINISHED signal
            String finishMsg = "EXAM_FINISHED";
            byte[] finishBuf = finishMsg.getBytes(StandardCharsets.UTF_8);
            DatagramPacket finishPacket = new DatagramPacket(finishBuf, finishBuf.length, group, UDP_PORT);
            mcastSocket.send(finishPacket);

            System.out.println("\n⏰ Countdown finished! Exam time ended.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
