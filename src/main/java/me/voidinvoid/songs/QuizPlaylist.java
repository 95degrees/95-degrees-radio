package me.voidinvoid.songs;

import me.voidinvoid.quiz.Quiz;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizPlaylist extends Playlist {

    private final Quiz quiz;
    private int currentQuestion = 0;
    private int currentQuestionProgress = 0;

    public QuizPlaylist(Quiz quiz) {
        super(quiz.getInternal());
        this.quiz = quiz;
    }

    @Override
    public void awaitLoad() {

    }

    @Override
    public void onActivate() {
        currentQuestion = 0;
        currentQuestionProgress = 0;
    }

    @Override
    public Song provideNextSong(boolean playJingle) {
        return null;
    }
}
