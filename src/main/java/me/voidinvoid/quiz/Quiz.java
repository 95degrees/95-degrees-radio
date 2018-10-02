package me.voidinvoid.quiz;

import java.util.List;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class Quiz {

    private String internal;
    private String title;
    private List<QuizQuestion> questions;
    private QuizType type;

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
}
