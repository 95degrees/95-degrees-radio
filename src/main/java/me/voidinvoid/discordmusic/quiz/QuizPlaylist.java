package me.voidinvoid.discordmusic.quiz;

import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.FormattingUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.managers.GuildController;

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
    private int currentQuestion;
    private QuizProgress quizProgress;
    private QuizQuestionProgress currentQuestionProgress;
    private boolean progressMade;
    private boolean automaticProgression;

    private boolean allowRepeat;

    private Message activeQuestionMessage, quizManagerMessage;

    private Map<QuizParticipant, QuizAnswer> currentAnswers;

    private List<QuizParticipant> remainingParticipants;

    private String nextProgressionAction;

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
    public String getStatusOverrideMessage() {
        return quiz.getTitle();
    }

    @Override
    public void onActivate() {
        currentQuestion = -1;
        quizProgress = QuizProgress.WAITING;
        currentQuestionProgress = null;
        activeQuestionMessage = null;

        allowRepeat = true;
        progressMade = false;
        automaticProgression = false;

        remainingParticipants = new ArrayList<>();
        currentAnswers = new HashMap<>();

        nextProgressionAction = "start question 1";

        quizManagerMessage = manager.getQuizManagerChannel().sendMessage(generateStatusMessage()).complete();
        quizManagerMessage.addReaction(ADVANCE_QUIZ_EMOTE).complete();

        manager.getTextChannel().sendMessage(getBaseEmbed(false).setTitle("The quiz is starting soon! 🎲").build()).queue();
    }

    public MessageEmbed generateStatusMessage() {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("❓ Quiz Control Panel")
                .setColor(Colors.ACCENT_QUIZ)
                .addField("Loaded quiz", quiz.getTitle(), true)
                .addField("Current state", "Quiz: " + quizProgress + (currentQuestionProgress == null ? "" : "\nQuestion: " + currentQuestionProgress), true)
                .addField("", "\u200b", false)
                .setFooter(quizProgress == QuizProgress.ENDED ? "🚫 Quiz has ended" : nextProgressionAction == null ? "🚫 Quiz cannot be advanced yet" : "Press " + ADVANCE_QUIZ_EMOTE + " to " + nextProgressionAction, null);

        boolean nobodyCorrect = remainingParticipants.stream().noneMatch(p -> currentAnswers.containsKey(p) && currentAnswers.get(p).isCorrect());

        if (quizProgress == QuizProgress.IN_PROGRESS) {
            QuizQuestion cq = quiz.getQuestions().get(currentQuestion);
            eb.addField("Current question (" + (currentQuestion + 1) + "/" + (quiz.getQuestions().size()) + ")", "Eligible contestants: " + (currentQuestion == 0 ? "<everyone>" : currentQuestionProgress == QuizQuestionProgress.DISPLAYING_ANSWERS ? "-" : remainingParticipants.stream().map(QuizParticipant::getName).collect(Collectors.joining(", "))) + "\n\n" + cq.getQuestion() + "\n\n" +
                    IntStream.range(0, cq.getAnswers().length)
                            .mapToObj(i -> {
                                QuizAnswer a = cq.getAnswers()[i];
                                return (a.isCorrect() ? "**" + a.getAnswer() + "**" : a.getAnswer()) + " (" + currentAnswers.values().stream().filter(v -> v.equals(a)).count() + ")";
                            }).collect(Collectors.joining("\n")), false);

            if (cq.getImageUrl() != null) {
                eb.setThumbnail(cq.getImageUrl());
            }

            if (remainingParticipants.size() == 0 || nobodyCorrect) {
                eb.addField("", "⚠ No participants have answered correctly" + (currentQuestionProgress == QuizQuestionProgress.IN_PROGRESS ? " so far" : ""), false);
            } else {
                eb.addField("", "✅ " + remainingParticipants.stream().filter(p -> currentAnswers.containsKey(p) && currentAnswers.get(p).isCorrect()).map(QuizParticipant::getName).collect(Collectors.joining(", ")) + " will make it to the next round", false);
            }
        }

        if (quizProgress == QuizProgress.ENDED) return eb.build();

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

    public void addParticipantAnswer(Member member, int answerIndex) {
        User user = member.getUser();
        if (currentQuestion == 0) {
            if (currentAnswers.keySet().stream().anyMatch(p -> p.getId().equals(user.getId())))
                return; //already answered

            QuizParticipant part = new QuizParticipant(user);

            addAnswer(part, quiz.getQuestions().get(currentQuestion).getAnswers()[answerIndex]);

            manager.getTextChannel().getGuild().getController().addSingleRoleToMember(member, manager.getQuizInGameRole()).queue();

        } else {
            //only accept their answer if they're a remaining participant
            remainingParticipants.stream().filter(p -> p.getId().equals(user.getId())).findAny()
                    .ifPresent(p -> addAnswer(p, quiz.getQuestions().get(currentQuestion).getAnswers()[answerIndex]));
        }
        //TODO HISTORY
    }

    private void addAnswer(QuizParticipant p, QuizAnswer answer) {
        if (currentAnswers.containsKey(p)) return;

        if (!remainingParticipants.contains(p) && answer.isCorrect()) remainingParticipants.add(p);

        currentAnswers.put(p, answer);

        quizManagerMessage.editMessage(generateStatusMessage()).queue(m -> quizManagerMessage = m);
    }

    @Override
    public Song provideNextSong(boolean playJingle) {
        if (!progressMade && automaticProgression) this.progress(false);
        if (!progressMade && !allowRepeat) return null;

        progressMade = false;

        if (quizProgress == QuizProgress.ENDED) {
            return manager.getWaitingSong();
            //todo something else maybe
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
        }

        if (currentQuestionProgress == QuizQuestionProgress.DISPLAYING_ANSWERS) {
            return manager.getAnswerSong();
        }

        return null;
    }

    private EmbedBuilder getBaseEmbed(boolean question) {
        return new EmbedBuilder()
                //.setAuthor("95 Degrees Trivia", "http://95degrees.cf", "https://cdn.discordapp.com/avatars/121387133821911040/86baaf30c545a20cf27b68b01dead28e.png")
                .setAuthor("95 Degrees Trivia" + (question ? (" - Q" + (currentQuestion + 1)) : ""), null, manager.getTextChannel().getJDA().getSelfUser().getAvatarUrl())
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

                activeQuestionMessage = manager.getTextChannel().sendMessage(getBaseEmbed(true)
                        .setDescription(IntStream.range(0, qc.getAnswers().length).mapToObj(i -> FormattingUtils.NUMBER_EMOTES.get(i) + " " + qc.getAnswers()[i].getAnswer()).collect(Collectors.joining("\n")))
                        .setFooter("React with the corresponding number to select your answer", null)
                        .setImage(qc.getImageUrl())
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

            if (activeQuestionMessage != null) { //shouldn't be null
                activeQuestionMessage.clearReactions().queue();
            }

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

                        return (a.isCorrect() ? CORRECT_EMOTE : INCORRECT_EMOTE) + " " + a.getAnswer() + " - " + currentAnswers.entrySet().stream().filter(e -> e.getValue().isCorrect()).count();
                    }).collect(Collectors.joining("\n")))
                    .build()).queue();

            /*manager.getTextChannel().sendMessage(getBaseEmbed()
                    .setDescription(currentAnswers.entrySet().stream().map(e -> e.getKey().getName() + " - ans " + e.getValue()).collect(Collectors.joining(", ")))
                    .build()).queue();*/

            requiresManualPlay = true;

            List<QuizParticipant> correctParticipants = currentAnswers.entrySet().stream().filter(e -> e.getValue().isCorrect()).map(Map.Entry::getKey).collect(Collectors.toList());

            GuildController controller = manager.getController();
            remainingParticipants.stream().filter(p -> !correctParticipants.contains(p)).forEach(p -> {
                try {
                    controller.addSingleRoleToMember(manager.getController().getGuild().getMemberById(p.getId()), manager.getQuizEliminatedRole()).queue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            remainingParticipants = correctParticipants;

            nextProgressionAction = currentQuestion + 1 >= quiz.getQuestions().size() || remainingParticipants.size() == 0 ? "play winner suspense music" : "start question " + (currentQuestion + 2);

        } else if (quizProgress == QuizProgress.WINNER_SUSPENSE) {
            quizProgress = QuizProgress.WINNER_ANNOUNCEMENT;

            List<String> res = remainingParticipants.stream().map(p -> "<@" + p.getId() + ">").collect(Collectors.toList());
            long numberOfWinners = res.size();
            String winners = String.join(",\n", res);

            //winners. winners.lastIndexOf("\n")

            manager.getTextChannel().sendMessage(getBaseEmbed(false)
                    .setTitle("Winners")
                    .setDescription(winners.isEmpty() ? "Nobody has won! 😢" : winners + (numberOfWinners == 1 ? " has" : " have") + " won! Congratulations! 🎉")
                    .build()).queue();

            nextProgressionAction = "end the quiz (will play waiting music again)";

        } else if (quizProgress == QuizProgress.WINNER_ANNOUNCEMENT) {
            quizProgress = QuizProgress.ENDED;

            manager.getTextChannel().sendMessage(getBaseEmbed(false)
                    .setTitle("The quiz has ended")
                    .build()).queue();

            nextProgressionAction = null;

            GuildController controller = manager.getController();
            controller.getGuild().getMembersWithRoles(manager.getQuizInGameRole()).forEach(m -> {
                controller.removeRolesFromMember(m, manager.getQuizInGameRole(), manager.getQuizEliminatedRole()).reason("Quiz has ended - removing roles").queue();
            });
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
