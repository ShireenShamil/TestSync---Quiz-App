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

    /**
     * Compute percentage score (0-100) for a given user based on stored answers.
     * If no answers, returns 0.
     */
    public static synchronized int computeScore(String username) {
        Map<Question, Integer> userAnswers = results.get(username);
        if (userAnswers == null || userAnswers.isEmpty()) return 0;

        int total = userAnswers.size();
        int correct = 0;
        for (Map.Entry<Question, Integer> e : userAnswers.entrySet()) {
            Question q = e.getKey();
            Integer ans = e.getValue();
            if (ans != null && ans == q.getCorrectOption()) correct++;
        }

        return (int) Math.round((correct * 100.0) / total);
    }

    /**
     * Return a map of username -> score (percentage).
     */
    public static synchronized Map<String, Integer> getAllScores() {
        Map<String, Integer> scores = new HashMap<>();
        for (String user : results.keySet()) {
            scores.put(user, computeScore(user));
        }
        return scores;
    }

    /**
     * Return detailed answers per user as a map: username -> (questionText -> answerIndex)
     */
    public static synchronized Map<String, Map<String, Integer>> getDetailedResults() {
        Map<String, Map<String, Integer>> detailed = new HashMap<>();
        for (Map.Entry<String, Map<Question, Integer>> e : results.entrySet()) {
            String user = e.getKey();
            Map<String, Integer> answers = new HashMap<>();
            for (Map.Entry<Question, Integer> qa : e.getValue().entrySet()) {
                answers.put(qa.getKey().getQuestionText(), qa.getValue());
            }
            detailed.put(user, answers);
        }
        return detailed;
    }
}
