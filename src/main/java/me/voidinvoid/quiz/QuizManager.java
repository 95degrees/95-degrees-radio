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
import me.voidinvoid.songs.QuizPlaylist;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongType;
import net.dv8tion.jda.core.entities.TextChannel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizManager implements SongEventListener {

    private static Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private Map<Quiz, QuizPlaylist> quizzes;
    private Path quizRoot;

    private TextChannel textChannel;

    private SocketIOServer server;
    private List<SocketIOClient> authenticatedClients = new ArrayList<>();

    private String serverCode = UUID.randomUUID().toString();

    private Song questionCountdownSong;
    private Song answerSong;
    private Song answerCorrectSong;
    private Song answerIncorrectSong;
    private Song waitingSong;

    private QuizPlaylist activeQuiz;

    public QuizManager(Path quizRoot, TextChannel textChannel) {

        this.quizRoot = quizRoot;
        this.textChannel = textChannel;

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
        //start_quiz (quiz internal id param)
        server.addEventListener("start_quiz", String.class, (c, o, ack) -> {
            if (!authenticatedClients.contains(c)) return;

            startQuiz(o);
        });

        //next_question (no param)
        server.addEventListener("next_question", Object.class, (c, o, ack) -> {
            if (!authenticatedClients.contains(c)) return;

            if (activeQuiz != null) {
                activeQuiz.progress();
            }
        });

        //next_question (no param)
        //same as previous on java end, but if there are any updates
        //in the future on java end, vue shouldn't need to change
        server.addEventListener("next_question", Object.class, (c, o, ack) -> {
            if (!authenticatedClients.contains(c)) return;

            if (activeQuiz != null) {
                activeQuiz.progress();
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
                if (activeQuiz.progress()) {
                    Radio.instance.getOrchestrator().playNextSong();
                }
            }
        });

        server.startAsync();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    public void reload() {
        quizzes = new HashMap<>();

        //todo DEBUG
        QuizPlaylist DEBUG_playlist = new QuizPlaylist(Quiz.__DEBUG_QUIZ, this);
        quizzes.put(Quiz.__DEBUG_QUIZ, DEBUG_playlist);
        Radio.instance.getOrchestrator().getPlaylists().add(DEBUG_playlist);
        Radio.instance.getOrchestrator().setActivePlaylist(DEBUG_playlist);
        ////////////

        try {
            Path soundsRoot = quizRoot.resolve("Sounds");
            questionCountdownSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("countdown.mp3").toFile());
            answerSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("answer.mp3").toFile());
            answerCorrectSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("answer-correct.mp3").toFile());
            answerIncorrectSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("answer-incorrect.mp3").toFile());
            waitingSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("waiting.mp3").toFile());

            Files.list(quizRoot).filter(p -> !Files.isDirectory(p)).forEach(q -> {
                try {
                    Quiz quiz = GSON.fromJson(new String(Files.readAllBytes(q)), Quiz.class);
                    QuizPlaylist playlist = new QuizPlaylist(quiz, this);
                    quizzes.put(quiz, playlist);
                    Radio.instance.getOrchestrator().getPlaylists().add(playlist);
                } catch (IOException e) {
                    System.out.println("Error loading quiz file " + q);
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Radio.instance.getOrchestrator().getPlaylists().addAll(quizzes.stream().map(q -> new QuizPlaylist(q, this)).collect(Collectors.toList()));
    }

    public boolean startQuiz(String internal) {
        QuizPlaylist quiz = quizzes.entrySet().stream().filter(kv -> kv.getKey().getInternal().equalsIgnoreCase(internal)).map(Map.Entry::getValue).findAny().orElse(null);

        if (quiz == null) return false;

        activeQuiz = quiz;

        if (server != null) {
            emitToAuthenticated("quiz_activated", activeQuiz.getInternal());
        }

        Radio.instance.getOrchestrator().setActivePlaylist(quiz);

        return true;
    }

    public void emitToAuthenticated(String key, Object value) {
        authenticatedClients.forEach(c -> c.sendEvent(key, value));
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (song.equals(questionCountdownSong)) {

        }
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

    public TextChannel getTextChannel() {
        return textChannel;
    }
}
