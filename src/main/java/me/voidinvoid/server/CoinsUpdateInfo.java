package me.voidinvoid.server;

public class CoinsUpdateInfo {

    public String userId;
    public int amount;
    public double elapsed;

    public CoinsUpdateInfo(String userId, int amount, double elapsed) {
        this.userId = userId;
        this.amount = amount;
        this.elapsed = elapsed;
    }
}
