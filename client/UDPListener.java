package client;

import java.net.*;
import javax.swing.JLabel;

public class UDPListener {
    public static void listen(int port, JLabel timerLabel) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                if (timerLabel != null) {
                    timerLabel.setText(msg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
