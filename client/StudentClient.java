package client;

import server.Question;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class StudentClient {

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final int UDP_PORT = 9876;

    private JFrame frame;
    private JTextArea questionArea;
    private JRadioButton[] optionButtons;
    private ButtonGroup group;
    private JButton submitButton;
    private JLabel timerLabel;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Question currentQuestion;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentClient().showLogin());
    }

    // --- LOGIN SCREEN ---
    private void showLogin() {
        JFrame loginFrame = new JFrame("TestSync Login");
        loginFrame.setSize(300, 200);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new GridLayout(3, 2));

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JButton loginBtn = new JButton("Login");

        loginFrame.add(new JLabel("Username:"));
        loginFrame.add(usernameField);
        loginFrame.add(new JLabel("Password:"));
        loginFrame.add(passwordField);
        loginFrame.add(new JLabel());
        loginFrame.add(loginBtn);

        loginBtn.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            try {
                connectToServer(username, password);
                loginFrame.dispose(); // close login frame
                createExamUI();
                // start UDP listener for timer
                new Thread(() -> UDPListener.listen(UDP_PORT, timerLabel)).start();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(loginFrame, "Connection failed: " + ex.getMessage());
            }
        });

        loginFrame.setLocationRelativeTo(null); // center on screen
        loginFrame.setVisible(true);
    }

    // --- CONNECT TO SERVER ---
    private void connectToServer(String username, String password) throws Exception {
        Socket socket = new Socket(SERVER_IP, SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        // authentication
        in.readObject(); // "Enter username:"
        out.writeObject(username);
        in.readObject(); // "Enter password:"
        out.writeObject(password);
        in.readObject(); // auth success message
    }

    // --- EXAM GUI ---
    private void createExamUI() {
        frame = new JFrame("TestSync");
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(10, 10));

        timerLabel = new JLabel("Time Left: --", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timerLabel.setForeground(Color.RED);
        frame.add(timerLabel, BorderLayout.NORTH);

        questionArea = new JTextArea();
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setEditable(false);
        questionArea.setFont(new Font("Arial", Font.PLAIN, 16));
        frame.add(questionArea, BorderLayout.CENTER);

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(4, 1, 5, 5));
        optionButtons = new JRadioButton[4];
        group = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            optionButtons[i] = new JRadioButton();
            group.add(optionButtons[i]);
            optionsPanel.add(optionButtons[i]);
        }
        frame.add(optionsPanel, BorderLayout.SOUTH);

        submitButton = new JButton("Submit Answer");
        submitButton.addActionListener(e -> submitAnswer());
        frame.add(submitButton, BorderLayout.EAST);

        frame.setLocationRelativeTo(null); // center on screen
        frame.setVisible(true);

        loadNextQuestion();
    }

    private void loadNextQuestion() {
        try {
            Object obj = in.readObject();
            if (obj instanceof String) {
                JOptionPane.showMessageDialog(frame, (String) obj);
                frame.dispose();
                return;
            }
            currentQuestion = (Question) obj;
            questionArea.setText(currentQuestion.getQuestionText());
            String[] opts = currentQuestion.getOptions();
            for (int i = 0; i < opts.length; i++) {
                optionButtons[i].setText(opts[i]);
            }
            group.clearSelection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void submitAnswer() {
        int selected = -1;
        for (int i = 0; i < optionButtons.length; i++) {
            if (optionButtons[i].isSelected()) {
                selected = i + 1; // option numbers start from 1
            }
        }
        if (selected == -1) {
            JOptionPane.showMessageDialog(frame, "Select an answer first!");
            return;
        }

        try {
            out.writeInt(selected);
            loadNextQuestion();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
