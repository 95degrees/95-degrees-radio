package me.voidinvoid.quiz;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizQuestion {

    private final String question;
    private final QuizAnswer[] answers;

    public QuizQuestion(String question, QuizAnswer... answers) {

        this.question = question;
        this.answers = answers;
    }

    public String getQuestion() {
        return question;
    }

    public QuizAnswer[] getAnswers() {
        return answers;
    }

    public QuizAnswer getCorrectAnswer() {
        for (QuizAnswer a : answers) {
            if (a.isCorrect()) return a;
        }

        return null;
    }
}
