package me.voidinvoid.karaoke;

import me.voidinvoid.utils.FormattingUtils;

public class Lyric {

    private String text;
    private double entryTime, length;

    public Lyric(String text, double entryTime, double length) {
        this.text = FormattingUtils.escapeMarkup(text.replace("&apos;", "'").replace("&quot;", "\"")); //todo temp
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
