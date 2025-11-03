package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ExamServer {
    private static final int PORT = 12345;
    private static List<Question> questions = new ArrayList<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws IOException {
        // Sample questions
        questions.add(new Question("What is 2+2?", new String[]{"3","4","5","6"}, 2));
        questions.add(new Question("Capital of Sri Lanka?", new String[]{"Colombo","Kandy","Galle","Jaffna"}, 1));
        questions.add(new Question("Java is ___", new String[]{"Programming language","Coffee","OS","Browser"}, 1));

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Exam Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            pool.execute(new ClientHandler(clientSocket));
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        public ClientHandler(Socket socket) { this.socket = socket; }

        public void run() {
            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // Simple authentication
                out.writeObject("Enter username:");
                String username = (String) in.readObject();
                out.writeObject("Enter password:");
                String password = (String) in.readObject();
                out.writeObject("Authentication successful! Starting exam...");

                // Send questions
                for (Question q : questions) {
                    out.writeObject(q);
                    int answer = in.readInt();
                    ResultManager.submitAnswer(username, q, answer);
                }

                out.writeObject("Exam completed! Thank you.");
                ResultManager.printAllResults();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
