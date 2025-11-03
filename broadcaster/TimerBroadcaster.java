package broadcaster;

import java.net.*;

public class TimerBroadcaster {
    public static void main(String[] args) {
        int udpPort = 9876;
        try (DatagramSocket socket = new DatagramSocket()) {
            int countdown = 60; // 60 seconds for demo
            while (countdown >= 0) {
                String msg = "Time left: " + countdown + " sec";
                byte[] buffer = msg.getBytes();
                InetAddress address = InetAddress.getByName("255.255.255.255"); // Broadcast
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, udpPort);
                socket.send(packet);
                Thread.sleep(1000);
                countdown--;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
