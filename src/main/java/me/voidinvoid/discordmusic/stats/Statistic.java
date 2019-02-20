package me.voidinvoid.discordmusic.stats;

import me.voidinvoid.discordmusic.utils.Formatting;

import java.util.function.Function;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public enum Statistic {

    COINS_EARNED("Coins Earned", false, a -> "Đ" + a),
    LISTEN_TIME("Listening Time", true, Formatting::getFormattedMinsTimeLabelled),
    SONGS_SUGGESTED("Songs Suggested", false, a -> a + "");

    private String displayName;
    private boolean createLeaderboard;
    private Function<Integer, String> format;

    Statistic(String displayName, boolean createLeaderboard, Function<Integer, String> format) {

        this.displayName = displayName;
        this.createLeaderboard = createLeaderboard;
        this.format = format;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isCreateLeaderboard() {
        return createLeaderboard;
    }

    public String format(int value) {
        return format.apply(value);
    }
}
