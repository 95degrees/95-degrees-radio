package me.voidinvoid.coins;

import me.voidinvoid.utils.ConsoleColor;
import net.dv8tion.jda.core.entities.User;

public class UserCoinTracker {

    private static final long COINS_PER_MINUTE = 1;

    private User user;
    private long startTime;
    private long resumedTime;
    private int earnedCoins;
    private long totalTime;
    private boolean frozen;

    public UserCoinTracker(User user, boolean frozen) {

        this.user = user;
        this.frozen = frozen;
        startTime = resumedTime = System.currentTimeMillis();

        System.out.println(ConsoleColor.YELLOW_BACKGROUND_BRIGHT + " COINS " + ConsoleColor.RESET_SPACE + "Started tracking coin gain for " + user);
    }

    public User getUser() {
        return user;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getEarnedCoins() {
        return earnedCoins;
    }

    public void credit() {
        if (!frozen) {
            long time = System.currentTimeMillis();
            totalTime += time - resumedTime;
        }

        int coinGain = (int) Math.floor(totalTime / 1000 / 60); //convert total from ms to minutes
        coinGain *= COINS_PER_MINUTE;

        earnedCoins += coinGain;

        System.out.println(ConsoleColor.YELLOW_BACKGROUND_BRIGHT + " COINS " + ConsoleColor.RESET_SPACE + "Credited " + user + ", " + earnedCoins);
    }

    public int getTotal() {
        credit();

        return earnedCoins;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void setEarnedCoins(int earnedCoins) {
        this.earnedCoins = earnedCoins;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {

        if (!this.frozen && frozen) {
            long time = System.currentTimeMillis();
            totalTime += time - resumedTime;
        }

        this.frozen = frozen;

        resumedTime = System.currentTimeMillis();
    }
}
