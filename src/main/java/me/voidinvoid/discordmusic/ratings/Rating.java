package me.voidinvoid.discordmusic.ratings;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public enum Rating {
    POSTIIVE("👍"),
    NEGATIVE("👎");

    private final String emote;

    Rating(String emote) {
        this.emote = emote;
    }

    public String getEmote() {
        return emote;
    }}
