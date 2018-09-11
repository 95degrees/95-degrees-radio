package me.voidinvoid.karaoke.lyrics;

import me.voidinvoid.utils.FormattingUtils;

public class LyricLine {

    private String text;
    private double entryTime, length;

    LyricLine(String text, double entryTime, double length) {
        this.text = FormattingUtils.escapeMarkup(text
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
