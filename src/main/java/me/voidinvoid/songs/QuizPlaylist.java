package me.voidinvoid.songs;

import me.voidinvoid.Radio;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.quiz.Quiz;
import me.voidinvoid.quiz.QuizManager;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizPlaylist extends Playlist {

    private final Quiz quiz;
    private QuizManager manager;
    private int currentQuestion = 0;
    private QuizProgress quizProgress;
    private QuizQuestionProgress currentQuestionProgress;
    private boolean progressMade;

    public QuizPlaylist(Quiz quiz, QuizManager manager) {
        super(quiz.getInternal());
        this.quiz = quiz;
        this.manager = manager;
    }

    @Override
    public void awaitLoad() {

    }

    @Override
    public void onActivate() {
        currentQuestion = 0;
        quizProgress = QuizProgress.NOT_STARTED;
        currentQuestionProgress = null;
    }

    @Override
    public Song provideNextSong(boolean playJingle) {
        if (!progressMade) return null; //so we're not repeating stuff over and over again
        progressMade = false;

        if (quizProgress == QuizProgress.ENDED) {
            Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage("Debug: round end").queue(); //todo DEBUG
            return null;
            //todo something
        }

        if (quizProgress == QuizProgress.NOT_STARTED) {
            Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage("Debug: waiting").queue(); //todo DEBUG
            return manager.getWaitingSong();
        }

        if (currentQuestionProgress == QuizQuestionProgress.IN_PROGRESS) {
            Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage("Debug: question countdown").queue(); //todo DEBUG
            return manager.getQuestionCountdownSong();
        }

        if (currentQuestionProgress == QuizQuestionProgress.ENDED) {
            Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage("Debug: quiz end").queue(); //todo DEBUG
            return null;
        }

        if (currentQuestionProgress == QuizQuestionProgress.DISPLAYING_ANSWERS) {
            Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage("Debug: displaying answers").queue(); //todo DEBUG
            return manager.getAnswerCorrectSong(); //todo
        }

        return null;
    }

    public boolean progress() { //if true, a call to playNextSong should be made - since we've missed the opportunity to do this naturally, i.e. right after the previous song had finished
        boolean requiresManualPlay = !progressMade;
        progressMade = true;

        if (currentQuestion == -1) { //-> play the waiting music
            currentQuestion++;
            quizProgress = QuizProgress.NOT_STARTED;

        } else if (currentQuestion > quiz.getQuestions().size()) { //-> quiz has ended
            quizProgress = QuizProgress.ENDED;

        } else if (currentQuestionProgress == null) { //-> right after the waiting music
            currentQuestionProgress = QuizQuestionProgress.IN_PROGRESS;
            quizProgress = QuizProgress.IN_PROGRESS;

        } else if (currentQuestionProgress == QuizQuestionProgress.IN_PROGRESS) { //-> right after the quiz countdown
            currentQuestionProgress = QuizQuestionProgress.ENDED;
            quizProgress = QuizProgress.IN_PROGRESS;

        } else if (currentQuestionProgress == QuizQuestionProgress.ENDED) { //-> right after the pause after the question ends
            currentQuestionProgress = QuizQuestionProgress.DISPLAYING_ANSWERS;
            quizProgress = QuizProgress.IN_PROGRESS;

        } else if (currentQuestionProgress == QuizQuestionProgress.DISPLAYING_ANSWERS) { //-> after we've shown the answers, go to the next question
            currentQuestion++;
            currentQuestionProgress = QuizQuestionProgress.IN_PROGRESS;
            quizProgress = QuizProgress.IN_PROGRESS;
        }

        return requiresManualPlay;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public enum QuizQuestionProgress {
        IN_PROGRESS,
        ENDED,
        DISPLAYING_ANSWERS
    }

    public enum QuizProgress {
        NOT_STARTED,
        IN_PROGRESS,
        ENDED
    }
}
