package client;

import java.net.*;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

public class UDPListener {
    private static Runnable onExamFinished = null;
    
    public static void setOnExamFinished(Runnable callback) {
        onExamFinished = callback;
    }
    
    public static void listen(int port, JLabel timerLabel) {
        try {
            DatagramSocket socket = new DatagramSocket(null); // Create unbound socket
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("0.0.0.0", port)); // Bind to all interfaces
            socket.setSoTimeout(2000); // 2 second timeout to update "Waiting..." message
            
            byte[] buffer = new byte[1024];
            boolean receivedFirstPacket = false;
            
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    receivedFirstPacket = true;
                    
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
                } catch (SocketTimeoutException e) {
                    // No packet received - show waiting message
                    if (!receivedFirstPacket && timerLabel != null) {
                        SwingUtilities.invokeLater(() -> 
                            timerLabel.setText("Timer: Waiting for broadcast...")
                        );
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
