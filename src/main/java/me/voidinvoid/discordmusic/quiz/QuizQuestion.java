package me.voidinvoid.discordmusic.quiz;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizQuestion {

    private String question;
    private String imageUrl;
    private String audioUrl;
    private QuizAnswer[] answers;

    public QuizQuestion(String question, String imageUrl, String audioUrl, QuizAnswer... answers) {

        this.question = question;
        this.imageUrl = imageUrl;
        this.audioUrl = audioUrl;
        this.answers = answers;
    }

    public QuizQuestion() {}

    public String getQuestion() {
        return question;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public QuizAnswer[] getAnswers() {
        return answers;
    }
}
