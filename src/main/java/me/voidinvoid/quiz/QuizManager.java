package me.voidinvoid.quiz;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.events.SongEventListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class QuizManager implements SongEventListener {

    private static Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private List<Quiz> quizzes;
    private Path quizRoot;

    private Quiz activeQuiz;

    public QuizManager(Path quizRoot) {

        this.quizRoot = quizRoot;
    }

    public void init() {
        quizzes = new ArrayList<>();

        try {
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
    }

    public boolean startQuiz(String internal) {
        Quiz quiz = quizzes.stream().filter(q -> q.getInternal().equalsIgnoreCase(internal)).findAny().orElse(null);

        if (quiz == null) return false;

        activeQuiz = quiz;

        return true;
    }

    public boolean isQuizActive() {
        return activeQuiz != null;
    }
}
