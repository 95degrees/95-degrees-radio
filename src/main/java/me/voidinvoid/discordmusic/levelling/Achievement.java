package me.voidinvoid.discordmusic.levelling;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public enum Achievement {
    //LISTEN_WITH_10_PEOPLE("Radio Rave", "Listen to the radio whilst as least 10 people are listening", 30),
    LISTEN_FOR_10_HOURS("Avid Listener", "Listen to the radio for 10 hours total", 60),
    OVER_LENGTH_LIMIT("Not Allowed", "Queue a song that's just 1 second over the length limit", 50),
    LISTEN_FOR_5_HOURS_AT_ONCE("In the Zone", "Listen to the radio for 5 or more hours at once", 100),
    RATE_SONG("Democracy", "Give a rating to a song", 30),
    MUTE_RADIO(":(", "Deafen yourself whilst in the radio text channel", 30),
    RICKROLL("Rickroller", "Rickroll the radio", 80),
    QUEUE_SONG_WITH_RRP("It's Magic", "Queue a song using the 95 Degrees Radio app", 80),
    USE_RDP("Very Rich Presence", "Use the 95 Degrees Radio app to show your listening status on Discord", 150);

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
