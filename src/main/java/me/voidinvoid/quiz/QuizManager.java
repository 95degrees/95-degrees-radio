package me.voidinvoid.quiz;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.Radio;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.events.SongEventListener;
import me.voidinvoid.songs.FileSong;
import me.voidinvoid.songs.Playlist;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongType;
import me.voidinvoid.utils.FormattingUtils;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.*;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizManager implements SongEventListener, EventListener {

    private static Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private Map<Quiz, QuizPlaylist> quizzes;
    private Path quizRoot;

    private TextChannel textChannel, quizManagerChannel;

    private SocketIOServer server;
    private List<SocketIOClient> authenticatedClients = new ArrayList<>();

    private String serverCode = UUID.randomUUID().toString();

    private Song questionCountdownSong;
    private Song answerSong;
    private Song answerCorrectSong;
    private Song answerIncorrectSong;
    private Song waitingSong;
    private Song winnerSuspenseSong;
    private Song winnerSong;

    private QuizPlaylist activeQuiz;

    public QuizManager(Path quizRoot, TextChannel textChannel, TextChannel quizManagerChannel) {

        this.quizRoot = quizRoot;
        this.textChannel = textChannel;
        this.quizManagerChannel = quizManagerChannel;

        reload();
    }

    public void runServer() {
        Configuration config = new Configuration();
        config.setHostname(RadioConfig.config.debug ? "127.0.0.1" : "0.0.0.0");
        config.setPort(RadioConfig.config.debug ? 9301 : 9501);

        SocketConfig sockets = new SocketConfig();
        sockets.setReuseAddress(true);

        config.setSocketConfig(sockets);

        server = new SocketIOServer(config);

        server.addEventListener("authenticate", String.class, (c, data, ack) -> {
            if (!data.equals(serverCode)) return;

            if (!authenticatedClients.contains(c)) authenticatedClients.add(c);

            c.sendEvent("quizzes", quizzes);
            if (activeQuiz != null) c.sendEvent("active_quiz", activeQuiz);
        });

        server.addEventListener("list_quizzes", Object.class, (c, o, ack) -> {
            if (!authenticatedClients.contains(c)) return;

            c.sendEvent("quizzes", quizzes);
        });

        //NEW
        //activate_quiz (quiz internal id param)
        server.addEventListener("start_quiz", String.class, (c, o, ack) -> {
            if (!authenticatedClients.contains(c)) return;

            startQuiz(o);
        });

        //next_question (no param)
        server.addEventListener("next_question", Object.class, (c, o, ack) -> {
            if (!authenticatedClients.contains(c)) return;

            if (activeQuiz != null) {
                activeQuiz.progress(false);
            }
        });

        //next_question (no param)
        //same as previous on java end, but if there are any updates
        //in the future on java end, vue shouldn't need to change
        server.addEventListener("next_question", Object.class, (c, o, ack) -> {
            if (!authenticatedClients.contains(c)) return;

            if (activeQuiz != null) {
                activeQuiz.progress(false);
            }
        });


        //OLD
        server.addEventListener("start_quiz", String.class, (c, o, ack) -> {
            if (!authenticatedClients.contains(c)) return;

            startQuiz(o);
        });

        server.addEventListener("make_quiz_progress", Object.class, (c, o, ack) -> {
            if (!authenticatedClients.contains(c)) return;

            if (activeQuiz != null) {
                if (activeQuiz.progress(false)) {
                    Radio.instance.getOrchestrator().playNextSong();
                }
            }
        });

        server.startAsync();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    public void loadDynamicQuiz(Path file) {
        QuizPlaylist quiz = loadQuiz(file);

        quizManagerChannel.sendMessage("Loaded quiz!").queue();

        startQuiz(quiz);

        try {
            Files.delete(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public QuizPlaylist loadQuiz(Path q) {
        try {
            Quiz quiz = GSON.fromJson(new String(Files.readAllBytes(q)), Quiz.class);

            return new QuizPlaylist(quiz, this);
        } catch (IOException e) {
            System.out.println("Error loading quiz file " + q);
            e.printStackTrace();
            return null;
        }
    }

    public void reload() {
        quizzes = new HashMap<>();

        //todo DEBUG
        /*QuizPlaylist DEBUG_playlist = new QuizPlaylist(Quiz.__DEBUG_QUIZ, this);
        quizzes.put(Quiz.__DEBUG_QUIZ, DEBUG_playlist);
        Radio.instance.getOrchestrator().getPlaylists().add(DEBUG_playlist);
        Radio.instance.getOrchestrator().setActivePlaylist(DEBUG_playlist);
        activeQuiz = DEBUG_playlist;*/
        ////////////

        try {
            Path soundsRoot = quizRoot.resolve("Sounds");
            questionCountdownSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("countdown.mp3").toFile());
            answerSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("answer.mp3").toFile());
            answerCorrectSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("answer-correct.mp3").toFile());
            answerIncorrectSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("answer-incorrect.mp3").toFile());
            waitingSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("waiting.mp3").toFile());
            winnerSuspenseSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("winner-suspense.mp3").toFile());
            winnerSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("winner.mp3").toFile());

            Files.list(quizRoot).filter(p -> !Files.isDirectory(p)).forEach(q -> {
                QuizPlaylist playlist = loadQuiz(q);
                if (playlist != null) {
                    quizzes.put(playlist.getQuiz(), playlist);
                    Radio.instance.getOrchestrator().getPlaylists().add(playlist);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Radio.instance.getOrchestrator().getPlaylists().addAll(quizzes.stream().map(q -> new QuizPlaylist(q, this)).collect(Collectors.toList()));
    }

    public boolean startQuiz(QuizPlaylist quiz) {
        if (quiz == null) return false;

        activeQuiz = quiz;

        if (server != null) {
            emitToAuthenticated("quiz_activated", activeQuiz.getInternal());
        }

        Radio.instance.getOrchestrator().setActivePlaylist(quiz);
        Radio.instance.getOrchestrator().playNextSong();

        return true;
    }

    public boolean startQuiz(String internal) {
        return startQuiz(quizzes.entrySet().stream().filter(kv -> kv.getKey().getInternal().equalsIgnoreCase(internal)).map(Map.Entry::getValue).findAny().orElse(null));
    }

    @Override
    public void onPlaylistChange(Playlist oldPlaylist, Playlist newPlaylist) {
        if (activeQuiz != null) {
            activeQuiz.getQuizManagerMessage().delete().reason("End of quiz").queue();
        }
    }

    public void emitToAuthenticated(String key, Object... value) {
        authenticatedClients.forEach(c -> c.sendEvent(key, value));
    }

    public boolean isQuizActive() {
        return activeQuiz != null;
    }

    public Song getQuestionCountdownSong() {
        return questionCountdownSong;
    }

    public Song getAnswerSong() {
        return answerSong;
    }

    public Song getAnswerCorrectSong() {
        return answerCorrectSong;
    }

    public Song getAnswerIncorrectSong() {
        return answerIncorrectSong;
    }

    public String getServerCode() {
        return serverCode;
    }

    public Song getWaitingSong() {
        return waitingSong;
    }

    public Song getWinnerSuspenseSong() {
        return winnerSuspenseSong;
    }

    public Song getWinnerSong() {
        return winnerSong;
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    public QuizPlaylist getActiveQuiz() {
        return activeQuiz;
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildMessageReactionAddEvent) {
            GuildMessageReactionAddEvent e = ((GuildMessageReactionAddEvent) ev);
            if (e.getUser().isBot()) return;

            if (activeQuiz == null) return;

            Message controlMsg = activeQuiz.getQuizManagerMessage();
            if (controlMsg != null && controlMsg.getId().equals(e.getMessageId())) {
                String emote = e.getReaction().getReactionEmote().getName();

                if (emote.equals(QuizPlaylist.ADVANCE_QUIZ_EMOTE)) {
                    if (activeQuiz.progress(true)) Radio.instance.getOrchestrator().playNextSong();
                    e.getReaction().removeReaction(e.getUser()).queue();
                }
                return;
            }

            Message questionMsg = activeQuiz.getActiveQuestionMessage();
            if (questionMsg == null) return;

            if (questionMsg.getId().equals(e.getMessageId())) {
                String emote = e.getReaction().getReactionEmote().getName();

                int ix = 0;
                for (String em : FormattingUtils.NUMBER_EMOTES) {
                    if (em.equals(emote)) {
                        activeQuiz.addParticipantAnswer(e.getUser(), ix);
                        e.getReaction().removeReaction(e.getUser()).queue();
                        return;
                    }
                    ix++;
                }
            }
        } else if (ev instanceof GuildMessageReceivedEvent) {
            GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) ev;

            if (e.getAuthor().isBot() || !e.getChannel().getId().equals(quizManagerChannel.getId())) return;

            if (e.getMessage().getAttachments().isEmpty()) return;

            Message.Attachment att = e.getMessage().getAttachments().get(0);

            if (!att.isImage() && att.getFileName().endsWith(".quiz")) {
                try {
                    Path file = Files.createTempDirectory("quiz-attachment-").resolve("quiz");
                    att.download(file.toFile());

                    loadDynamicQuiz(file);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public TextChannel getQuizManagerChannel() {
        return quizManagerChannel;
    }
}
