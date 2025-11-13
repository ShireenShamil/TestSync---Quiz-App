package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ExamServer {
    private static final int PORT = 12345;
    private static List<Question> questions = new ArrayList<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(10);
    private static volatile boolean examStarted = false;
    private static List<ClientHandler> waitingClients = Collections.synchronizedList(new ArrayList<>());
    
    // Public method to programmatically start the exam (called from AdminHTTPServer)
    public static synchronized void startExamProgrammatically() {
        if (!examStarted) {
            examStarted = true;
            ExamState.setState("RUNNING");
            System.out.println("\n🚀 EXAM STARTED (via Admin Dashboard)! Notifying " + waitingClients.size() + " connected student(s)...\n");
            notifyAllClients();
        }
    }

    public static void main(String[] args) throws IOException {
        // Sample questions
        questions.add(new Question("What is 2+2?", new String[]{"3","4","5","6"}, 2));
        questions.add(new Question("Capital of Sri Lanka?", new String[]{"Colombo","Kandy","Galle","Jaffna"}, 1));
        questions.add(new Question("Java is ___", new String[]{"Programming language","Coffee","OS","Browser"}, 1));

        // Start a thread to monitor for exam start command
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("\n⏳ Server ready. Type 'START' and press Enter to begin the exam for all connected students:");
            while (true) {
                String command = scanner.nextLine().trim().toUpperCase();
                if (command.equals("START") && !examStarted) {
                    examStarted = true;
                    ExamState.setState("RUNNING");
                    System.out.println("\n🚀 EXAM STARTED! Notifying " + waitingClients.size() + " connected student(s)...\n");
                    notifyAllClients();
                    break;
                }
            }
        }).start();

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("✅ Exam Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(clientSocket);
            pool.execute(handler);
        }
    }

    private static synchronized void notifyAllClients() {
        // Notify all waiting clients
        for (ClientHandler client : waitingClients) {
            client.startExam();
        }
        waitingClients.clear();
        
        // Broadcast START signal to TimerBroadcaster via UDP
        try {
            DatagramSocket udpSocket = new DatagramSocket();
            String startSignal = "START_EXAM";
            byte[] buffer = startSignal.getBytes();
            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 9877);
            udpSocket.send(packet);
            udpSocket.close();
            System.out.println("📡 START signal sent to TimerBroadcaster");
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not send START signal to broadcaster: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String username;
        private boolean readyToStart = false;

        public ClientHandler(Socket socket) { 
            this.socket = socket; 
        }

        public synchronized void startExam() {
            readyToStart = true;
            notify();
        }

        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Authenticate with UserManager
                out.writeObject("Enter username:");
                username = (String) in.readObject();
                out.writeObject("Enter password:");
                String password = (String) in.readObject();
                
                // Validate credentials
                if (!UserManager.authenticate(username, password)) {
                    out.writeObject("Authentication failed! Invalid username or password.");
                    socket.close();
                    return;
                }
                
                out.writeObject("Authentication successful!");
                
                // Add to waiting list and track connected students
                waitingClients.add(this);
                ExamState.incrementConnectedStudents();
                out.writeObject("WAITING"); // Signal client to show waiting screen
                out.flush();
                
                System.out.println("👤 " + username + " connected and waiting for exam to start... (Total connected: " + ExamState.getConnectedStudents() + ")");
                
                // Wait until exam starts
                synchronized (this) {
                    while (!readyToStart && !examStarted) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                // Send START signal
                out.writeObject("START");
                out.flush();
                System.out.println("📝 Sending questions to " + username);

                // Send questions
                for (int q_num = 0; q_num < questions.size(); q_num++) {
                    Question q = questions.get(q_num);
                    System.out.println("[ExamServer] Sending question " + (q_num + 1) + " to " + username);
                    out.writeObject(q);
                    out.flush();
                    
                    try {
                        int answer = in.readInt();
                        System.out.println("[ExamServer] Received answer from " + username + ": " + answer);
                        ResultManager.submitAnswer(username, q, answer);
                    } catch (EOFException e) {
                        System.out.println("[ExamServer] " + username + " disconnected while answering questions");
                        ExamState.decrementConnectedStudents();
                        return;
                    }
                }

                // compute score and send to client
                int score = ResultManager.calculateScore(username, questions);
                out.writeObject("RESULT:" + score);
                out.flush();

                out.writeObject("Exam completed! Thank you.");
                out.flush();
                System.out.println("[ExamServer] Exam completed for " + username + " (score=" + score + "%)");
                ResultManager.printAllResults();
                ExamState.decrementConnectedStudents();
                socket.close();
            } catch (EOFException | SocketException e) {
                // Client disconnected (possibly due to time expiry) - this is normal
                System.out.println("⏰ " + username + " disconnected (exam time may have expired)");
                ExamState.decrementConnectedStudents();
            } catch (Exception e) {
                System.err.println("Error handling client " + username + ": " + e.getMessage());
                e.printStackTrace();
                ExamState.decrementConnectedStudents();
            }
        }
    }
}
