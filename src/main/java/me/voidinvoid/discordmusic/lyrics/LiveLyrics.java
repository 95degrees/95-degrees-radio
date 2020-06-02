package me.voidinvoid.discordmusic.lyrics;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LiveLyrics {

    public static final LyricLine EMPTY_LINE = new LyricLine(0, "...");

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("mm:ss.SS");

    private List<LyricLine> lines = new ArrayList<>();

    //[00:01.53] Derulo\n[00:02.67] Whine fa me, darlin' (Oh yeah)\n[00:03.97] Way you move ya spine is alarmin' (oh yeah)\n

    public LiveLyrics(String rawString) {
        var lines = rawString.split("\n");

        for (var line : lines) {
            var time = line.substring(1, 9);

            String[] timeSplit = time.split("[:.]");

            long ms = TimeUnit.MINUTES.toMillis(Integer.parseInt(timeSplit[0]));
            ms += TimeUnit.SECONDS.toMillis(Integer.parseInt(timeSplit[1]));
            ms += Integer.parseInt(timeSplit[2]) * 10;

            var text = line.substring(11);

            this.lines.add(new LyricLine(ms, text));
        }
    }

    public LyricLine getCurrentLine(long progressMs) {
        LyricLine activeLine = EMPTY_LINE;

        for (var line : lines) {
            if (line.getTime() > progressMs) return activeLine;
            activeLine = line;
        }

        return activeLine;
    }
}
