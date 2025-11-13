package client;

import java.net.*;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;

/**
 * Multicast-based UDP listener so multiple clients on the same host
 * receive the same timer updates.
 */
public class UDPListener {
    private static Runnable onExamFinished = null;
    private static final String MULTICAST_GROUP = "230.0.0.0";

    public static void setOnExamFinished(Runnable callback) {
        onExamFinished = callback;
    }

    public static void listen(int port, JLabel timerLabel) {
        InetAddress group;
        try {
            group = InetAddress.getByName(MULTICAST_GROUP);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        MulticastSocket socket = null;
        try {
            // Create socket without binding, set reuse before bind so multiple processes can bind same port
            socket = new MulticastSocket((SocketAddress) null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            // join group (simple overload)
            socket.joinGroup(group);
            socket.setSoTimeout(2000);

            byte[] buffer = new byte[1024];
            boolean receivedFirstPacket = false;

            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    receivedFirstPacket = true;

                    if (msg.equals("EXAM_FINISHED")) {
                        SwingUtilities.invokeLater(() -> {
                            if (timerLabel != null) timerLabel.setText("Time's Up!");
                            JOptionPane.showMessageDialog(null, "⏰ Time's up! Exam will auto-submit.", "Exam Finished", JOptionPane.INFORMATION_MESSAGE);
                            if (onExamFinished != null) onExamFinished.run();
                        });
                        break;
                    }

                    if (timerLabel != null) {
                        String finalMsg = msg;
                        SwingUtilities.invokeLater(() -> timerLabel.setText(finalMsg));
                    }
                } catch (SocketTimeoutException e) {
                    if (!receivedFirstPacket && timerLabel != null) {
                        SwingUtilities.invokeLater(() -> timerLabel.setText("Timer: Waiting for broadcast..."));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try { socket.leaveGroup(group); } catch (Exception ignored) {}
                socket.close();
            }
        }
    }
}
