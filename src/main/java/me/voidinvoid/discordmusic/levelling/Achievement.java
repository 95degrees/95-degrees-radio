package me.voidinvoid.discordmusic.levelling;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public enum Achievement {
    LISTEN_WITH_10_PEOPLE("Radio Rave", "Listen to the radio whilst as least 10 people are listening", 30),
    LISTEN_FOR_10_HOURS("Musicbrain", "Listen to the radio for 10 hours total", 60),
    USE_RDP("RDP", "Use the 95 Degrees Radio app to show your listening status on Discord", 100);

    private final String display;
    private final String description;
    private final int reward;

    Achievement(String display, String description, int reward) {

        this.display = display;
        this.description = description;
        this.reward = reward;
    }

    public String getDisplay() {
        return display;
    }

    public String getDescription() {
        return description;
    }

    public int getReward() {
        return reward;
    }
}
