package me.voidinvoid.discordmusic.guardian.requests;

/**
 * Guardian/Radio - 13/12/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class AddExperienceRequest {

    private String userId;
    private String channelId;
    private int amount;

    public AddExperienceRequest(String userId, String channelId, int amount) {

        this.userId = userId;
        this.channelId = channelId;
        this.amount = amount;
    }

    public AddExperienceRequest() {

    }

    public String getUserId() {
        return userId;
    }

    public String getChannelId() {
        return channelId;
    }

    public int getAmount() {
        return amount;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
