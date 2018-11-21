package me.voidinvoid.discordmusic.quiz;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizQuestion {

    private String question;
    private String imageUrl;
    private QuizAnswer[] answers;

    public QuizQuestion(String question, String imageUrl, QuizAnswer... answers) {

        this.question = question;
        this.imageUrl = imageUrl;
        this.answers = answers;
    }

    public QuizQuestion() {}

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
        if (answers == null) return -1;
        for (QuizAnswer a : answers) {
            if (a.isCorrect()) return i;
            i++;
        }

        return -1;
    }
}
