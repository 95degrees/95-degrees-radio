package me.voidinvoid.discordmusic.lyrics;

public class LyricLine {

    private final long time;
    private final String content;

    public LyricLine(long time, String content) {

        this.time = time;
        this.content = content;
    }

    public long getTime() {
        return time;
    }

    public String getContent() {
        return content;
    }
}
