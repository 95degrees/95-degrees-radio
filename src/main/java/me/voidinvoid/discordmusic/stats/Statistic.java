package me.voidinvoid.discordmusic.stats;

import me.voidinvoid.discordmusic.utils.Formatting;

import java.util.function.Function;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public enum Statistic {

    COINS_EARNED("Coins Earned", "Coins", false, a -> "Đ" + a),
    LISTEN_TIME("Listening Time", "Time", true, Formatting::getFormattedMinsTimeLabelled),
    SONGS_SUGGESTED("Songs Suggested", "Songs", false, a -> a + "");

    private String displayName;
    private String statName;
    private boolean createLeaderboard;
    private Function<Integer, String> format;

    Statistic(String displayName, String statName, boolean createLeaderboard, Function<Integer, String> format) {

        this.displayName = displayName;
        this.statName = statName;
        this.createLeaderboard = createLeaderboard;
        this.format = format;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getStatName() {
        return statName;
    }

    public boolean isCreateLeaderboard() {
        return createLeaderboard;
    }

    public String format(int value) {
        return format.apply(value);
    }
}
