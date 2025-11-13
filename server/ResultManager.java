package server;

import java.util.*;

public class ResultManager {
    private static Map<String, Map<Question, Integer>> results = new HashMap<>();

    public static synchronized void submitAnswer(String username, Question question, int answer) {
        results.putIfAbsent(username, new HashMap<>());
        results.get(username).put(question, answer);
        System.out.println("[ResultManager] Answer received from " + username + ": Q='" + question.getQuestionText() + "', Answer=" + answer);
        System.out.println("[ResultManager] Total submissions so far: " + results.size() + ", User answers: " + results.get(username).size());
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
     * Return a formatted report of all results as a String.
     * This is used by the Admin HTTP-like server to return results to an admin client.
     */
    public static synchronized String getResultsReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("----- All Exam Results -----\n");
        if (results.isEmpty()) {
            sb.append("No results submitted yet.\n");
            return sb.toString();
        }
        for (Map.Entry<String, Map<Question, Integer>> entry : results.entrySet()) {
            String user = entry.getKey();
            sb.append("Results for ").append(user).append(":\n");
            Map<Question, Integer> userResults = entry.getValue();
            for (Map.Entry<Question, Integer> r : userResults.entrySet()) {
                Question q = r.getKey();
                Integer ans = r.getValue();
                sb.append("- ").append(q.getQuestionText())
                  .append(" | Answer: ").append(ans == null ? "N/A" : ans)
                  .append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Return results as JSON for API endpoints.
     */
    public static synchronized String getResultsReportJson() {
        System.out.println("[ResultManager] getResultsReportJson() called - Total results: " + results.size());

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"submissions\": [\n");

        if (results.isEmpty()) {
            System.out.println("[ResultManager] No results to return");
            sb.append("  ],\n");
            sb.append("  \"totalSubmissions\": 0\n");
            sb.append("}");
            return sb.toString();
        }

        boolean first = true;
        for (Map.Entry<String, Map<Question, Integer>> entry : results.entrySet()) {
            if (!first) sb.append(",\n");
            first = false;

            String user = entry.getKey();
            Map<Question, Integer> userResults = entry.getValue();

            // compute score
            int total = userResults.size();
            int correct = 0;
            for (Map.Entry<Question, Integer> r : userResults.entrySet()) {
                Question q = r.getKey();
                Integer ans = r.getValue();
                if (ans != null && q.getCorrectOption() == ans) correct++;
            }
            int scorePercent = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);

            sb.append("    {\n");
            sb.append("      \"student\": \"").append(user).append("\",\n");
            sb.append("      \"scorePercent\": ").append(scorePercent).append(",\n");
            sb.append("      \"answers\": [\n");

            boolean firstAns = true;
            for (Map.Entry<Question, Integer> r : userResults.entrySet()) {
                if (!firstAns) sb.append(",\n");
                firstAns = false;

                Question q = r.getKey();
                Integer ans = r.getValue();
                boolean isCorrect = (ans != null && q.getCorrectOption() == ans);
                sb.append("        {\n");
                sb.append("          \"question\": \"").append(escapeJson(q.getQuestionText())).append("\",\n");
                sb.append("          \"answer\": ").append(ans == null ? "null" : ans).append(",\n");
                sb.append("          \"correct\": ").append(isCorrect).append("\n");
                sb.append("        }");
            }
            sb.append("\n      ]\n");
            sb.append("    }");
        }

        sb.append("\n  ],\n");
        sb.append("  \"totalSubmissions\": ").append(results.size()).append("\n");
        sb.append("}");
        return sb.toString();
    }
    
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }

    /**
     * Calculate score percent for a student using the provided list of questions.
     * Returns integer percent (0-100).
     */
    public static synchronized int calculateScore(String username, java.util.List<Question> questions) {
        Map<Question, Integer> userResults = results.get(username);
        if (userResults == null || questions == null || questions.isEmpty()) return 0;
        int total = questions.size();
        int correct = 0;
        for (Question q : questions) {
            Integer ans = userResults.get(q);
            if (ans != null && q.getCorrectOption() == ans) correct++;
        }
        return total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
    }
}
