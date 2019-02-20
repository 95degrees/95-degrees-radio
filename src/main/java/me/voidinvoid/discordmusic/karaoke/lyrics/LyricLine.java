package me.voidinvoid.discordmusic.karaoke.lyrics;

import me.voidinvoid.discordmusic.utils.Formatting;

public class LyricLine {

    private String text;
    private double entryTime, length;

    LyricLine(String text, double entryTime, double length) {
        this.text = Formatting.escape(text
                .replace("&apos;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replace("&quot;", "\""));
        this.entryTime = entryTime;
        this.length = length;
    }

    public String getText() {
        return text;
    }

    public double getEntryTime() {
        return entryTime;
    }

    public double getLength() {
        return length;
    }
}
