package me.voidinvoid.discordmusic.stats;

import org.jetbrains.annotations.NotNull;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class LeaderboardEntry implements Comparable {

    private final String user;
    private final int value;

    public LeaderboardEntry(String user, int value) {

        this.user = user;
        this.value = value;
    }

    public String getUser() {
        return user;
    }

    public int getValue() {
        return value;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        return ((LeaderboardEntry) o).value - value;
    }
}
