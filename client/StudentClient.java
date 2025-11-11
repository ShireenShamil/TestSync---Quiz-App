package client;

import server.Question;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class StudentClient {

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final int UDP_PORT = 9876;

    private JFrame frame;
    private JTextArea questionArea;
    private JRadioButton[] optionButtons;
    private ButtonGroup group;
    private JButton submitButton;
    private JLabel timerLabel, questionCountLabel;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Question currentQuestion;
    private int totalQuestions = 0;
    private boolean examStarted = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentClient().showLogin());
    }

    // --- LOGIN SCREEN ---
    
    private void showLogin() {
        JFrame loginFrame = new JFrame("TestSync - Login");
        loginFrame.setSize(350, 220);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JButton loginBtn = new JButton("Login");

        JLabel title = new JLabel("🔒 TestSync Login", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        loginFrame.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        loginFrame.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        loginFrame.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        loginFrame.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        loginFrame.add(passwordField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        loginFrame.add(loginBtn, gbc);

        loginBtn.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            try {
                connectToServer(username, password);
                loginFrame.dispose();
                
                // Initialize timer label first
                timerLabel = new JLabel("Timer: Waiting for broadcast...", SwingConstants.CENTER);
                timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                timerLabel.setForeground(new Color(75, 110, 175));
                
                // Show waiting screen
                showWaitingScreen();
                
                // Set callback for when exam time finishes
                UDPListener.setOnExamFinished(() -> autoFinishExam());
                
                // Start UDP listener after timer label is initialized
                new Thread(() -> UDPListener.listen(UDP_PORT, timerLabel)).start();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(loginFrame, "❌ Login failed: " + ex.getMessage());
            }
        });

        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }

    // --- CONNECT TO SERVER ---
    private void connectToServer(String username, String password) throws Exception {
        Socket socket = new Socket(SERVER_IP, SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        in.readObject(); // prompt
        out.writeObject(username);
        in.readObject(); // prompt
        out.writeObject(password);
        
        String response = (String) in.readObject(); // success or failure message
        if (response.contains("failed")) {
            socket.close();
            throw new Exception(response);
        }
        
        // Check for WAITING status
        String status = (String) in.readObject();
        if (status.equals("WAITING")) {
            // Client will show waiting screen
            // Start a thread to wait for START signal
            new Thread(() -> {
                try {
                    String startSignal = (String) in.readObject();
                    if (startSignal.equals("START")) {
                        SwingUtilities.invokeLater(() -> {
                            examStarted = true;
                            createExamUI();
                            loadNextQuestion();
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // --- WAITING SCREEN ---
    private void showWaitingScreen() {
        JFrame waitingFrame = new JFrame("🕐 TestSync - Please Wait");
        waitingFrame.setSize(500, 350);
        waitingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        waitingFrame.setLayout(new BorderLayout());
        waitingFrame.getContentPane().setBackground(new Color(245, 246, 252));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        JLabel iconLabel = new JLabel("⏳", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 72));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Quiz Yet to Start", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel messageLabel = new JLabel("Please wait for the teacher to start the exam...", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        messageLabel.setForeground(Color.GRAY);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Use the already initialized timerLabel
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Animated dots
        Timer animationTimer = new Timer(500, null);
        final String[] dots = {"", ".", "..", "..."};
        final int[] index = {0};
        
        animationTimer.addActionListener(e -> {
            if (!examStarted) {
                messageLabel.setText("Please wait for the teacher to start the exam" + dots[index[0]]);
                index[0] = (index[0] + 1) % dots.length;
            } else {
                animationTimer.stop();
                waitingFrame.dispose();
            }
        });
        animationTimer.start();

        centerPanel.add(iconLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        centerPanel.add(titleLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        centerPanel.add(messageLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 25)));
        centerPanel.add(timerLabel);

        waitingFrame.add(centerPanel, BorderLayout.CENTER);
        waitingFrame.setLocationRelativeTo(null);
        waitingFrame.setVisible(true);
    }

    // --- EXAM SCREEN ---
    private void createExamUI() {
        frame = new JFrame("🧠 TestSync Exam");
        frame.setSize(700, 550);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(new Color(245, 246, 252));

        // --- TOP PANEL ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(75, 110, 175));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Update existing timerLabel styling for exam screen
        timerLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        timerLabel.setForeground(Color.WHITE);

        questionCountLabel = new JLabel("Question: 1", SwingConstants.LEFT);
        questionCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        questionCountLabel.setForeground(Color.WHITE);

        topPanel.add(questionCountLabel, BorderLayout.WEST);
        topPanel.add(timerLabel, BorderLayout.EAST);
        frame.add(topPanel, BorderLayout.NORTH);

        // --- MAIN PANEL (Question + Options) ---
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        mainPanel.setBackground(Color.WHITE);

        questionArea = new JTextArea();
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setEditable(false);
        questionArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        questionArea.setBackground(new Color(250, 250, 250));
        questionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        mainPanel.add(questionArea);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // --- OPTIONS ---
        JPanel optionsPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        optionsPanel.setBackground(Color.WHITE);
        optionButtons = new JRadioButton[4];
        group = new ButtonGroup();

        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            optionButtons[i].setFont(new Font("Segoe UI", Font.PLAIN, 15));
            optionButtons[i].setBackground(new Color(250, 250, 250));
            optionButtons[i].setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));
            optionButtons[i].setFocusPainted(false);
            group.add(optionButtons[i]);
            optionsPanel.add(optionButtons[i]);
        }

        mainPanel.add(optionsPanel);
        frame.add(mainPanel, BorderLayout.CENTER);

        // --- SUBMIT BUTTON ---
        submitButton = new JButton("Submit Answer");
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        submitButton.setBackground(new Color(75, 110, 175));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFocusPainted(false);
        submitButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        submitButton.addActionListener(e -> submitAnswer());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(new Color(245, 246, 252));
        bottomPanel.add(submitButton);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Don't automatically load questions - wait for START signal
    }

    private void loadNextQuestion() {
        try {
            Object obj = in.readObject();
            if (obj instanceof String msg) {
                if (msg.startsWith("RESULT:")) {
                    showResult(msg);
                    frame.dispose();
                    return;
                }
                JOptionPane.showMessageDialog(frame, msg);
                return;
            }

            currentQuestion = (Question) obj;
            totalQuestions++;
            questionArea.setText(currentQuestion.getQuestionText());
            String[] opts = currentQuestion.getOptions();

            for (int i = 0; i < opts.length; i++) {
                optionButtons[i].setText(opts[i]);
            }

            group.clearSelection();
            questionCountLabel.setText("Question: " + totalQuestions);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void submitAnswer() {
        int selected = -1;
        for (int i = 0; i < optionButtons.length; i++) {
            if (optionButtons[i].isSelected()) {
                selected = i + 1;
            }
        }
        if (selected == -1) {
            JOptionPane.showMessageDialog(frame, "⚠️ Please select an answer first!");
            return;
        }

        try {
            out.writeInt(selected);
            out.flush();
            loadNextQuestion();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- RESULT SCREEN ---
    private void showResult(String resultMessage) {
        String[] parts = resultMessage.split(":");
        int score = Integer.parseInt(parts[1].trim());

        JFrame resultFrame = new JFrame("🎉 Exam Completed");
        resultFrame.setSize(350, 220);
        resultFrame.setLayout(new GridBagLayout());
        resultFrame.getContentPane().setBackground(Color.WHITE);
        resultFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridy = 0;

        JLabel msg = new JLabel("✅ Exam Completed Successfully!", SwingConstants.CENTER);
        msg.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JLabel scoreLabel = new JLabel("Your Score: " + score + "%", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        scoreLabel.setForeground(new Color(0, 128, 0));

        JButton exitBtn = new JButton("Exit");
        exitBtn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        exitBtn.addActionListener(e -> System.exit(0));

        gbc.gridy++;
        resultFrame.add(msg, gbc);
        gbc.gridy++;
        resultFrame.add(scoreLabel, gbc);
        gbc.gridy++;
        resultFrame.add(exitBtn, gbc);

        resultFrame.setLocationRelativeTo(null);
        resultFrame.setVisible(true);
    }
    
    // --- AUTO FINISH EXAM WHEN TIME EXPIRES ---
    private void autoFinishExam() {
        try {
            if (frame != null && frame.isVisible()) {
                // Close the exam window
                frame.dispose();
                
                // Show time's up message
                JOptionPane.showMessageDialog(null, 
                    "⏰ Time's up! Your answers have been submitted.", 
                    "Exam Finished", 
                    JOptionPane.INFORMATION_MESSAGE);
                
                // Close connection
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
