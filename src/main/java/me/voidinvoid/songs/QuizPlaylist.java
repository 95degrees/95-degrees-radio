package me.voidinvoid.songs;

import me.voidinvoid.quiz.*;
import me.voidinvoid.utils.Colors;
import me.voidinvoid.utils.FormattingUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizPlaylist extends Playlist {

    public static final String CORRECT_EMOTE = "✅", INCORRECT_EMOTE = "❌", ADVANCE_QUIZ_EMOTE = "⏭";

    private final Quiz quiz;
    private QuizManager manager;
    private int currentQuestion = -1;
    private QuizProgress quizProgress;
    private QuizQuestionProgress currentQuestionProgress;
    private boolean progressMade;
    private boolean automaticProgression;

    private boolean allowRepeat = true;

    private int correctAnswerIndex;

    private Message activeQuestionMessage, quizManagerMessage;

    private Map<QuizParticipant, Integer> currentAnswers = new HashMap<>();

    private List<QuizParticipant> remainingParticipants;

    private String nextProgressionAction = "start question 1";

    public Message getQuizManagerMessage() {
        return quizManagerMessage;
    }

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
        currentQuestion = -1;
        quizProgress = QuizProgress.WAITING;
        currentQuestionProgress = null;

        remainingParticipants = new ArrayList<>();

        quizManagerMessage = manager.getQuizManagerChannel().sendMessage(generateStatusMessage()).complete();
        quizManagerMessage.addReaction(ADVANCE_QUIZ_EMOTE).complete();
    }

    public MessageEmbed generateStatusMessage() {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("❓ Quiz Control Panel")
                .setColor(Colors.ACCENT_QUIZ)
                .addField("Loaded quiz", quiz.getTitle(), true)
                .addField("Current state", "Quiz: " + quizProgress + (currentQuestionProgress == null ? "" : "\nQuestion: " + currentQuestionProgress), true)
                .addField("", "\u200b", false)
                .setFooter("Press " + ADVANCE_QUIZ_EMOTE + " to " + nextProgressionAction, null);

        boolean nobodyCorrect = remainingParticipants.stream().noneMatch(p -> currentAnswers.containsKey(p) && currentAnswers.get(p) == correctAnswerIndex);

        if (quizProgress == QuizProgress.IN_PROGRESS) {
            QuizQuestion cq = quiz.getQuestions().get(currentQuestion);
            eb.addField("Current question (" + (currentQuestion + 1) + "/" + (quiz.getQuestions().size()) + ")", "Eligible contestants: " + (currentQuestion == 0 ? "<everyone>" : remainingParticipants.stream().map(QuizParticipant::getName).collect(Collectors.joining(", "))) + "\n\n" + cq.getQuestion() + "\n\n" + Arrays.stream(cq.getAnswers()).map(a -> a.isCorrect() ? "**" + a.getAnswer() + "**" : a.getAnswer()).collect(Collectors.joining("\n")), false);

            if (remainingParticipants.size() == 0 || nobodyCorrect) {
                eb.addField("", "⚠ No participants have answered correctly" + (currentQuestionProgress == QuizQuestionProgress.IN_PROGRESS ? " so far" : ""), false);
            } else {
                eb.addField("", "✅ " + remainingParticipants.stream().filter(p -> currentAnswers.getOrDefault(p, -1) == correctAnswerIndex).map(QuizParticipant::getName).collect(Collectors.joining(", ")) + " will make it to the next round", false);
            }
        }

        eb.addField("", "\u200b", false);

        if (quizProgress == QuizProgress.IN_PROGRESS && currentQuestionProgress != QuizQuestionProgress.IN_PROGRESS && nobodyCorrect) {
            eb.addField("The quiz will end after this round", "Nobody answered the previous question correctly", false);
        } else if (quizProgress == QuizProgress.WINNER_SUSPENSE || quizProgress == QuizProgress.WINNER_ANNOUNCEMENT) {
            String winners = remainingParticipants.stream().map(QuizParticipant::getName).collect(Collectors.joining(", "));

            eb.addField("Game winners (" + remainingParticipants.size() + ")", winners.isEmpty() ? "<nobody>" : winners, false);
        } else if (currentQuestion <= quiz.getQuestions().size() - 2) {
            QuizQuestion nq = quiz.getQuestions().get(currentQuestion + 1);
            eb.addField("Preview of next question (" + (currentQuestion + 2) + "/" + (quiz.getQuestions().size()) + ")", nq.getQuestion() + "\n\n" + Arrays.stream(nq.getAnswers()).map(a -> a.isCorrect() ? "**" + a.getAnswer() + "**" : a.getAnswer()).collect(Collectors.joining("\n")), false);
        } else {
            eb.addField("The quiz will end after this round", "This is the final question", false);
        }

        return eb.build();
    }

    public void addParticipantAnswer(User user, int answerIndex) {
        if (currentQuestion == 0) {
            if (currentAnswers.keySet().stream().anyMatch(p -> p.getId().equals(user.getId())))
                return; //already answered
            QuizParticipant part = new QuizParticipant(user);
            addAnswer(part, answerIndex);
        } else {
            remainingParticipants.stream().filter(p -> p.getId().equals(user.getId())).findAny()
                    .ifPresent(p -> {
                        addAnswer(p, answerIndex);
                    });
        }
        //TODO HISTORY
    }

    private void addAnswer(QuizParticipant p, int answerIndex) {
        if (currentAnswers.containsKey(p)) return;

        if (!remainingParticipants.contains(p) && answerIndex == correctAnswerIndex) remainingParticipants.add(p);

        currentAnswers.put(p, answerIndex);

        quizManagerMessage.editMessage(generateStatusMessage()).queue(m -> quizManagerMessage = m);
    }

    @Override
    public Song provideNextSong(boolean playJingle) {
        if (!progressMade && automaticProgression) this.progress(false);
        if (!progressMade && !allowRepeat) return null;

        progressMade = false;

        if (quizProgress == QuizProgress.ENDED) {
            return manager.getAnswerSong();
            //todo something
        }

        if (quizProgress == QuizProgress.WAITING) {
            return manager.getWaitingSong();
        }

        if (quizProgress == QuizProgress.WINNER_SUSPENSE) {
            return manager.getWinnerSuspenseSong();
        }

        if (quizProgress == QuizProgress.WINNER_ANNOUNCEMENT) {
            return manager.getWinnerSong();
        }

        if (currentQuestionProgress == QuizQuestionProgress.IN_PROGRESS) {
            new Thread(() -> {
                try {
                    Thread.sleep(10500);
                    progress(false);
                } catch (Exception ignored) {
                }
            }).start();

            return manager.getQuestionCountdownSong();
        }

        if (currentQuestionProgress == QuizQuestionProgress.ENDED) {
            return null;
            //return manager.getAnswerSong();
        }

        if (currentQuestionProgress == QuizQuestionProgress.DISPLAYING_ANSWERS) {
            return manager.getAnswerSong();
            //return manager.getAnswerCorrectSong(); //todo
        }

        return null;
    }

    private EmbedBuilder getBaseEmbed(boolean question) {
        return new EmbedBuilder()
                //.setAuthor("95 Degrees Trivia", "http://95degrees.cf", "https://cdn.discordapp.com/avatars/121387133821911040/86baaf30c545a20cf27b68b01dead28e.png")
                .setAuthor("95 Degrees Trivia" + (question ? (" - Q" + (currentQuestion + 1)) : ""))
                .setTitle(question ? quiz.getQuestions().get(currentQuestion).getQuestion() : null)
                .setColor(Colors.ACCENT_QUIZ);
    }

    public boolean progress(boolean fromControlPanel) { //if true, a call to playNextSong should be made - since we've missed the opportunity to do this naturally, i.e. right after the previous song had finished
        boolean requiresManualPlay = !progressMade;
        progressMade = true;
        allowRepeat = false;
        automaticProgression = false;

        if (fromControlPanel && nextProgressionAction == null) return false;

        /*if (quizProgress == QuizProgress.NOT_STARTED) { //-> play the waiting music
            quizProgress = QuizProgress.WAITING;

            manager.getTextChannel().sendMessage(getBaseEmbed().setTitle("Quiz is starting soon").build()).queue();
            allowRepeat = true;
        } else */
        /*if (currentQuestion > quiz.getQuestions().size()) { //-> quiz has ended
            quizProgress = QuizProgress.ENDED;

            manager.getTextChannel().sendMessage(getBaseEmbed().setTitle("Quiz has ended").build()).queue();

            nextProgressionAction = null;

        } else */
        if ((quizProgress == QuizProgress.WAITING && currentQuestionProgress == null) || currentQuestionProgress == QuizQuestionProgress.DISPLAYING_ANSWERS) { //-> right after the waiting music, start question 1, or continue to next questions
            currentQuestion++;
            currentQuestionProgress = QuizQuestionProgress.IN_PROGRESS;
            quizProgress = QuizProgress.IN_PROGRESS;

            currentAnswers.clear();

            if (currentQuestion >= quiz.getQuestions().size() || (currentQuestion > 0 && remainingParticipants.isEmpty())) {
                currentQuestionProgress = null;
                quizProgress = QuizProgress.WINNER_SUSPENSE;

                nextProgressionAction = "announce the winner";
            } else {

                QuizQuestion qc = quiz.getQuestions().get(currentQuestion);

                correctAnswerIndex = qc.getCorrectAnswerIndex();

                activeQuestionMessage = manager.getTextChannel().sendMessage(getBaseEmbed(true)
                        .setDescription(IntStream.range(0, qc.getAnswers().length).mapToObj(i -> FormattingUtils.NUMBER_EMOTES.get(i) + " " + qc.getAnswers()[i].getAnswer()).collect(Collectors.joining("\n")))
                        .setFooter("React with the corresponding number to select your answer", null)
                        .build())
                        .complete();

                IntStream.range(0, qc.getAnswers().length).forEach(i -> activeQuestionMessage.addReaction(FormattingUtils.NUMBER_EMOTES.get(i)).queue());

                manager.emitToAuthenticated("set_question", remainingParticipants); //todo

                allowRepeat = false;
                nextProgressionAction = null;
            }
            //automaticProgression = true;

        } else if (currentQuestionProgress == QuizQuestionProgress.IN_PROGRESS) { //-> right after the quiz countdown
            currentQuestionProgress = QuizQuestionProgress.ENDED;
            quizProgress = QuizProgress.IN_PROGRESS;

            manager.getTextChannel().sendMessage(getBaseEmbed(true)
                    .setTitle("The results are in!")
                    .build()).queue();

            nextProgressionAction = "show question results";

        } else if (currentQuestionProgress == QuizQuestionProgress.ENDED) { //-> right after the pause after the question ends
            currentQuestionProgress = QuizQuestionProgress.DISPLAYING_ANSWERS;
            quizProgress = QuizProgress.IN_PROGRESS;

            QuizQuestion qc = quiz.getQuestions().get(currentQuestion);

            manager.getTextChannel().sendMessage(getBaseEmbed(true)
                    .setDescription(IntStream.range(0, qc.getAnswers().length).mapToObj(i -> {
                        QuizAnswer a = qc.getAnswers()[i];

                        return (a.isCorrect() ? CORRECT_EMOTE : INCORRECT_EMOTE) + " " + a.getAnswer() + " - " + currentAnswers.entrySet().stream().filter(e -> e.getValue() == i).count();
                    }).collect(Collectors.joining("\n")))
                    .build()).queue();

            /*manager.getTextChannel().sendMessage(getBaseEmbed()
                    .setDescription(currentAnswers.entrySet().stream().map(e -> e.getKey().getName() + " - ans " + e.getValue()).collect(Collectors.joining(", ")))
                    .build()).queue();*/

            nextProgressionAction = currentQuestion + 2 >= quiz.getQuestions().size() || remainingParticipants.size() == 0 ? "play winner suspense music" : "start question " + (currentQuestion + 2);

            remainingParticipants = currentAnswers.entrySet().stream().filter(e -> e.getValue() == correctAnswerIndex).map(Map.Entry::getKey).collect(Collectors.toList());
        } else if (quizProgress == QuizProgress.WINNER_SUSPENSE) {
            quizProgress = QuizProgress.WINNER_ANNOUNCEMENT;

            String winners = remainingParticipants.stream().map(p -> "<@" + p.getId() + ">").collect(Collectors.joining(", "));

            manager.getTextChannel().sendMessage(getBaseEmbed(false)
                    .setTitle("Winners")
                    .setDescription(winners.isEmpty() ? "no-one lol" : winners)
                    .build()).queue();

        } else if (quizProgress == QuizProgress.WINNER_ANNOUNCEMENT) {
            quizProgress = QuizProgress.ENDED;

            manager.getTextChannel().sendMessage(getBaseEmbed(false)
                    .setTitle("The quiz has ended")
                    .build()).queue();
        }

        quizManagerMessage = quizManagerMessage.editMessage(generateStatusMessage()).complete();

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

    public Message getActiveQuestionMessage() {
        return currentQuestionProgress == QuizQuestionProgress.IN_PROGRESS ? activeQuestionMessage : null;
    }

    public enum QuizQuestionProgress {
        IN_PROGRESS("Question in progress"),
        ENDED("Question ended"),
        DISPLAYING_ANSWERS("Displaying answers");

        private final String display;

        QuizQuestionProgress(String display) {

            this.display = display;
        }


        @Override
        public String toString() {
            return display;
        }
    }

    public enum QuizProgress {
        //NOT_STARTED,
        WAITING("Waiting"),
        IN_PROGRESS("In progress"),
        WINNER_SUSPENSE("Winner suspense music"),
        WINNER_ANNOUNCEMENT("Winner announcement"),
        ENDED("Ended");

        private final String display;

        QuizProgress(String display) {

            this.display = display;
        }


        @Override
        public String toString() {
            return display;
        }
    }
}
