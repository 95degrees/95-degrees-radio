package me.voidinvoid.quiz;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.Radio;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.events.SongEventListener;
import me.voidinvoid.server.MemberInfo;
import me.voidinvoid.server.RadioInfo;
import me.voidinvoid.songs.FileSong;
import me.voidinvoid.songs.QuizPlaylist;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizManager implements SongEventListener {

    private static Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private List<Quiz> quizzes;
    private Path quizRoot;

    private SocketIOServer server;
    private List<SocketIOClient> authenticatedClients = new ArrayList<>();

    private String serverCode = UUID.randomUUID().toString();

    private Song questionCountdownSong;
    private Song answerSong;
    private Song answerCorrectSong;
    private Song answerIncorrectSong;

    private Quiz activeQuiz;

    public QuizManager(Path quizRoot) {

        this.quizRoot = quizRoot;

        reload();
    }

    public void runServer() {
        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
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

        server.startAsync();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    public void reload() {
        quizzes = new ArrayList<>();

        try {
            Path soundsRoot = quizRoot.resolve("Sounds");
            questionCountdownSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("countdown.mp3").toFile());
            answerSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("answer.mp3").toFile());
            answerCorrectSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("answer-correct.mp3").toFile());
            answerIncorrectSong = new FileSong(SongType.QUIZ, soundsRoot.resolve("answer-incorrect.mp3").toFile());

            Files.list(quizRoot).filter(p -> !Files.isDirectory(p)).forEach(q -> {
                try {
                    quizzes.add(GSON.fromJson(new String(Files.readAllBytes(q)), Quiz.class));
                } catch (IOException e) {
                    System.out.println("Error loading quiz file " + q);
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        Radio.instance.getOrchestrator().getPlaylists().addAll(quizzes.stream().map(QuizPlaylist::new).collect(Collectors.toList()));
    }

    public boolean startQuiz(String internal) {
        Quiz quiz = quizzes.stream().filter(q -> q.getInternal().equalsIgnoreCase(internal)).findAny().orElse(null);

        if (quiz == null) return false;

        activeQuiz = quiz;

        if (server != null) {
            authenticatedClients.forEach(c -> c.sendEvent("active_quiz", activeQuiz));
        }

        return true;
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
}
