package me.voidinvoid.discordmusic.notifications;

import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public enum Notification {

    STREAKS("Streaks", "messages when you complete your daily streak", false),
    INVALID_NAMES("Invalid Names", "warnings when you have an invalid name that needs to be reset", true),
    ACHIEVEMENTS("Achievements", "messages when you unlock an achievement", true),
    SUGGESTION_REPLIES("Suggestion Replies", "messages when staff members reply to your suggestions", true),
    RADIO_EARNINGS("Radio Earnings", "messages containing your Degreecoin earnings after listening to the radio", false);

    private final String name;
    private final String description;
    private final boolean defaultValue;

    Notification(String name, String description, boolean defaultValue) {

        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }

    public boolean is(Member member) {
        return Service.of(NotificationManager.class).isEnabled(member, this);
    }

    public boolean is(User user) {
        return Service.of(NotificationManager.class).isEnabled(user, this);
    }
}
