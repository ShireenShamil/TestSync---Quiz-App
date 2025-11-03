package server;

import java.io.Serializable;

public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    private String questionText;
    private String[] options;
    private int correctOption;

    public Question(String questionText, String[] options, int correctOption) {
        this.questionText = questionText;
        this.options = options;
        this.correctOption = correctOption;
    }

    public String getQuestionText() { return questionText; }
    public String[] getOptions() { return options; }
    public int getCorrectOption() { return correctOption; }
}
