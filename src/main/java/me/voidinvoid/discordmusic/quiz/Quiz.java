package me.voidinvoid.discordmusic.quiz;

import java.util.Arrays;
import java.util.List;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class Quiz {

    private String internal;
    private String title;
    private List<QuizQuestion> questions;
    private QuizType type; //todo

    public String getInternal() {
        return internal;
    }

    public String getTitle() {
        return title;
    }

    public List<QuizQuestion> getQuestions() {
        return questions;
    }

    public QuizType getType() {
        return type;
    }

    public static Quiz __DEBUG_QUIZ;

    static {
        __DEBUG_QUIZ = new Quiz();

        __DEBUG_QUIZ.internal = "DEBUG";
        __DEBUG_QUIZ.title = "Debug quiz";
        __DEBUG_QUIZ.type = QuizType.BATTLE_ROYALE;
        __DEBUG_QUIZ.questions = Arrays.asList(
                new QuizQuestion("What is the capital city of France?", null, null, new QuizAnswer("Paris", true), new QuizAnswer("London", false), new QuizAnswer("Madrid", false)),
                new QuizQuestion("whats this flag lol", "c", null, new QuizAnswer("England", false), new QuizAnswer("Scotland", true), new QuizAnswer("Australia", false)),
                new QuizQuestion("meaningful question 3", null, null, new QuizAnswer("wrong", false), new QuizAnswer("right", true), new QuizAnswer("wrong again", false)),
                new QuizQuestion("meaningful question 4", null, null, new QuizAnswer("wrong", false), new QuizAnswer("right", true), new QuizAnswer("wrong again", false)),
                new QuizQuestion("meaningful question 5", null, null, new QuizAnswer("wrong", false), new QuizAnswer("right", true), new QuizAnswer("wrong again", false)),
                new QuizQuestion("meaningful question 6", null, null, new QuizAnswer("wrong", false), new QuizAnswer("right", true), new QuizAnswer("wrong again", false)),
                new QuizQuestion("meaningful question 7", null, null, new QuizAnswer("wrong", false), new QuizAnswer("right", true), new QuizAnswer("wrong again", false)),
                new QuizQuestion("meaningful question 8", null, null, new QuizAnswer("wrong", false), new QuizAnswer("right", true), new QuizAnswer("wrong again", false))
        ); //IntStream.range(0, 10).mapToObj(i -> new QuizQuestion("Question " + i, new QuizAnswer("CORRECT", true), new QuizAnswer("INCORRECT-1", false), new QuizAnswer("INCORRECT-2", false))).collect(Collectors.toList());
    }
}
