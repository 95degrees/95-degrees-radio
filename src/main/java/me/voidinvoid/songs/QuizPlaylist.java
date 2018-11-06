package me.voidinvoid.songs;

import me.voidinvoid.Radio;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.quiz.Quiz;
import me.voidinvoid.quiz.QuizManager;
import me.voidinvoid.quiz.QuizQuestion;
import me.voidinvoid.utils.Colors;
import me.voidinvoid.utils.FormattingUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizPlaylist extends Playlist {

    private static final String CORRECT_EMOTE = "✅", INCORRECT_EMOTE = "❌";

    private final Quiz quiz;
    private QuizManager manager;
    private int currentQuestion = -1;
    private QuizProgress quizProgress;
    private QuizQuestionProgress currentQuestionProgress;
    private boolean progressMade;

    private int correctAnswerIndex;

    private Message lastQuizMessage;

    public QuizPlaylist(Quiz quiz, QuizManager manager) {
        super(quiz.getInternal());
        this.name = quiz.getTitle();
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
        this.progress(); //todo temp
        if (!progressMade) return null; //so we're not repeating stuff over and over again
        progressMade = false;

        if (quizProgress == QuizProgress.ENDED) {
            Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage("Debug: quiz end").queue(); //todo DEBUG
            return null;
            //todo something
        }

        if (quizProgress == QuizProgress.WAITING) {
            Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage("Debug: waiting").queue(); //todo DEBUG
            return manager.getWaitingSong();
        }

        if (currentQuestionProgress == QuizQuestionProgress.IN_PROGRESS) {
            Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage("Debug: question countdown").queue(); //todo DEBUG
            return manager.getQuestionCountdownSong();
        }

        if (currentQuestionProgress == QuizQuestionProgress.ENDED) {
            Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage("Debug: round end").queue(); //todo DEBUG
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

        if (quizProgress == QuizProgress.NOT_STARTED) { //-> play the waiting music
            quizProgress = QuizProgress.WAITING;

            manager.getTextChannel().sendMessage("**temp** waiting").queue();

        } else if (currentQuestion > quiz.getQuestions().size()) { //-> quiz has ended
            quizProgress = QuizProgress.ENDED;

            manager.getTextChannel().sendMessage("**temp** ended").queue();

        } else if (currentQuestionProgress == null || currentQuestionProgress == QuizQuestionProgress.DISPLAYING_ANSWERS) { //-> right after the waiting music, start question 1, or continue to next questions
            currentQuestion++;
            currentQuestionProgress = QuizQuestionProgress.IN_PROGRESS;
            quizProgress = QuizProgress.IN_PROGRESS;

            QuizQuestion qc = quiz.getQuestions().get(currentQuestion);

            manager.getTextChannel().sendMessage(new EmbedBuilder()
                    .setAuthor("95 Degrees Trivia - Q" + (currentQuestion + 1))
                    .setTitle(qc.getQuestion())
                    .setColor(Colors.ACCENT_QUIZ)
                    .setDescription(IntStream.range(0, qc.getAnswers().length).mapToObj(i -> FormattingUtils.NUMBER_EMOTES.get(i) + " " + qc.getAnswers()[i].getAnswer()).collect(Collectors.joining("\n")))
                    .build()).queue();

        } else if (currentQuestionProgress == QuizQuestionProgress.IN_PROGRESS) { //-> right after the quiz countdown
            currentQuestionProgress = QuizQuestionProgress.ENDED;
            quizProgress = QuizProgress.IN_PROGRESS;

            manager.getTextChannel().sendMessage(new EmbedBuilder()
                    .setAuthor("95 Degrees Trivia - Q" + (currentQuestion + 1))
                    .setTitle("The results are in!")
                    .setColor(Colors.ACCENT_QUIZ)
                    .build()).queue();

        } else if (currentQuestionProgress == QuizQuestionProgress.ENDED) { //-> right after the pause after the question ends
            currentQuestionProgress = QuizQuestionProgress.DISPLAYING_ANSWERS;
            quizProgress = QuizProgress.IN_PROGRESS;

            QuizQuestion qc = quiz.getQuestions().get(currentQuestion);

            manager.getTextChannel().sendMessage(new EmbedBuilder()
                    .setAuthor("95 Degrees Trivia - Q" + (currentQuestion + 1))
                    .setColor(Colors.ACCENT_QUIZ)
                    .setDescription(Arrays.stream(qc.getAnswers()).map(a -> (a.isCorrect() ? CORRECT_EMOTE : INCORRECT_EMOTE) + " " + a.getAnswer() + " - TODO").collect(Collectors.joining("\n")))
                    .build()).queue();
        } else if (currentQuestionProgress == QuizQuestionProgress.DISPLAYING_ANSWERS) { //-> after we've shown the answers, go to the next question
            currentQuestion++;
            currentQuestionProgress = QuizQuestionProgress.IN_PROGRESS;
            quizProgress = QuizProgress.IN_PROGRESS;

            manager.emitToAuthenticated("question_change", currentQuestion, System.currentTimeMillis(), System.currentTimeMillis() + 10000, remainingParticipants  ); //todo
        }

        return requiresManualPlay;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public int getCurrentQuestion() {
        return currentQuestion;
    }

    public QuizProgress getQuizProgress() {
        return quizProgress;
    }

    public QuizQuestionProgress getCurrentQuestionProgress() {
        return currentQuestionProgress;
    }

    public enum QuizQuestionProgress {
        IN_PROGRESS,
        ENDED,
        DISPLAYING_ANSWERS
    }

    public enum QuizProgress {
        NOT_STARTED,
        WAITING,
        IN_PROGRESS,
        ENDED
    }
}
