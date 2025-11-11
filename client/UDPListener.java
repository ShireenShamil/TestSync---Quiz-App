package client;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

/**
 * UDPListener using Java NIO (DatagramChannel + ByteBuffer)
 * Demonstrates non-blocking I/O for receiving UDP broadcasts
 */
public class UDPListener {
    private static Runnable onExamFinished = null;
    
    public static void setOnExamFinished(Runnable callback) {
        onExamFinished = callback;
    }
    
    public static void listen(int port, JLabel timerLabel) {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.socket().bind(new InetSocketAddress(port));
            channel.configureBlocking(false); // Non-blocking mode
            
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            boolean receivedFirstPacket = false;
            int noDataCounter = 0;
            
            System.out.println("📡 NIO DatagramChannel listening on port " + port + " (non-blocking)");
            
            while (true) {
                buffer.clear();
                SocketAddress sender = channel.receive(buffer);
                
                if (sender != null) {
                    // Data received
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    String msg = new String(data, StandardCharsets.UTF_8);
                    receivedFirstPacket = true;
                    noDataCounter = 0;
                    
                    // Check for exam finished signal
                    if (msg.equals("EXAM_FINISHED")) {
                        SwingUtilities.invokeLater(() -> {
                            if (timerLabel != null) {
                                timerLabel.setText("Time's Up!");
                            }
                            JOptionPane.showMessageDialog(null, 
                                "⏰ Time's up! Exam will auto-submit.", 
                                "Exam Finished", 
                                JOptionPane.INFORMATION_MESSAGE);
                            
                            if (onExamFinished != null) {
                                onExamFinished.run();
                            }
                        });
                        break;
                    }
                    
                    if (timerLabel != null) {
                        String finalMsg = msg;
                        SwingUtilities.invokeLater(() -> timerLabel.setText(finalMsg));
                    }
                } else {
                    // No data received (non-blocking mode)
                    noDataCounter++;
                    
                    // Show waiting message if no data for 20 iterations (~2 seconds)
                    if (!receivedFirstPacket && noDataCounter > 20 && timerLabel != null) {
                        SwingUtilities.invokeLater(() -> 
                            timerLabel.setText("Timer: Waiting for broadcast...")
                        );
                        noDataCounter = 0; // Reset counter
                    }
                    
                    // Small sleep to prevent busy waiting in non-blocking mode
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
