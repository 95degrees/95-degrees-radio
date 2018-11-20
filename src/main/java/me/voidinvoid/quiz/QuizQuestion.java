package me.voidinvoid.quiz;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizQuestion {

    private final String question;
    private final String imageUrl;
    private final QuizAnswer[] answers;

    public QuizQuestion(String question, String imageUrl, QuizAnswer... answers) {

        this.question = question;
        this.imageUrl = imageUrl;
        this.answers = answers;
    }

    public String getQuestion() {
        return question;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public QuizAnswer[] getAnswers() {
        return answers;
    }

    public int getCorrectAnswerIndex() {
        int i = 0;
        for (QuizAnswer a : answers) {
            if (a.isCorrect()) return i;
            i++;
        }

        return -1;
    }
}
