package me.voidinvoid.quiz;

import java.util.List;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2018
 */
public class Quiz {

    private String title;
    private List<QuizQuestion> questions;
    private QuizType type;

    public Quiz(String title, List<QuizQuestion> questions, QuizType type) {
        this.title = title;

        this.questions = questions;
        this.type = type;
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
