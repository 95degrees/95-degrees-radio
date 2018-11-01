package me.voidinvoid.quiz;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizAnswer {

    private final String answer;
    private final boolean correct;

    public QuizAnswer(String answer, boolean correct) {

        this.answer = answer;
        this.correct = correct;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isCorrect() {
        return correct;
    }
}
