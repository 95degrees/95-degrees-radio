package me.voidinvoid.discordmusic.quiz;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizAnswer {

    private String answer;
    private boolean correct;

    public QuizAnswer(String answer, boolean correct) {

        this.answer = answer;
        this.correct = correct;
    }

    public QuizAnswer() {}

    public String getAnswer() {
        return answer;
    }

    public boolean isCorrect() {
        return correct;
    }
}
