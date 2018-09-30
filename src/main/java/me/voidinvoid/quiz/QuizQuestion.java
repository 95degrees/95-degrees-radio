package me.voidinvoid.quiz;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizQuestion {

    private final String question;
    private final String correct;
    private final String incorrectA;
    private final String incorrectB;

    public QuizQuestion(String question, String correct, String incorrectA, String incorrectB) {

        this.question = question;
        this.correct = correct;
        this.incorrectA = incorrectA;
        this.incorrectB = incorrectB;
    }
}
