package server;

import java.util.*;

public class ResultManager {
    private static Map<String, Map<Question, Integer>> results = new HashMap<>();

    public static synchronized void submitAnswer(String username, Question question, int answer) {
        results.putIfAbsent(username, new HashMap<>());
        results.get(username).put(question, answer);
        System.out.println("Answer received from " + username + ": " + answer);
    }

    public static void printAllResults() {
        System.out.println("\n----- All Exam Results -----");
        for (String user : results.keySet()) {
            System.out.println("Results for " + user + ":");
            results.get(user).forEach((q, ans) ->
                System.out.println(q.getQuestionText() + " - Answer: " + ans)
            );
        }
    }
}
