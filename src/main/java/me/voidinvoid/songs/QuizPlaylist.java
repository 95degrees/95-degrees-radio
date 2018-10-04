package me.voidinvoid.songs;

import me.voidinvoid.quiz.Quiz;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizPlaylist extends Playlist {

    private final Quiz quiz;

    public QuizPlaylist(Quiz quiz) {
        super(quiz.getInternal());
        this.quiz = quiz;
    }

    @Override
    public void awaitLoad() {

    }

    @Override
    public Song provideNextSong(boolean playJingle) {
        return null;
    }
}
