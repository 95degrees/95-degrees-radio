package me.voidinvoid.discordmusic.coins;

import me.voidinvoid.discordmusic.utils.ConsoleColor;
import net.dv8tion.jda.api.entities.User;

public class UserCoinTracker {

    private static final int COINS_PER_MINUTE = 1;
    private static final int MAX_COINS = 300;

    private User user;
    private long startTime;
    private long resumedTime;
    private int earnedCoins;
    private long totalTime;
    private long creditedTime;
    private boolean frozen;
    private double multiplier;

    public UserCoinTracker(User user, boolean frozen, double multiplier) {

        this.user = user;
        this.frozen = frozen;
        this.multiplier = multiplier;

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

    public void credit(double multiplier) {
        if (!frozen) {
            long time = System.currentTimeMillis();
            totalTime += time - resumedTime;
        }

        resumedTime = System.currentTimeMillis();

        int coinGain = (int) Math.floor((totalTime - creditedTime) / 1000F / 60F); //convert total from ms to minutes
        coinGain *= COINS_PER_MINUTE;
        coinGain *= multiplier;

        earnedCoins += coinGain;

        creditedTime = totalTime;

        System.out.println(ConsoleColor.YELLOW_BACKGROUND_BRIGHT + " COINS " + ConsoleColor.RESET_SPACE + "Credited " + user + ", " + earnedCoins);
    }

    public void setMultiplier(double multiplier) {
        credit(this.multiplier);

        this.multiplier = multiplier;
    }

    public int getTotal() {
        credit(multiplier);

        return Math.min(MAX_COINS, earnedCoins);
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
