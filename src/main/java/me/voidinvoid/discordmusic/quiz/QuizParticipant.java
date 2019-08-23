package me.voidinvoid.discordmusic.quiz;

import net.dv8tion.jda.api.entities.User;

public class QuizParticipant {

    private String id;
    private String avatarUrl;
    private String name;

    public QuizParticipant(User user) {
        id = user.getId();
        avatarUrl = user.getAvatarUrl();
        name = user.getName();
    }

    public String getId() {
        return id;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getName() {
        return name;
    }
}
