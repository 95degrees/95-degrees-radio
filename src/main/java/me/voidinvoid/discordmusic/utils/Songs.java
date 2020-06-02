package me.voidinvoid.discordmusic.utils;

public final class Songs {

    public static String deyoutubeifySong(String song) {

        return song.toLowerCase()
                .replaceAll("[()\\[\\]{}]", "")
                .replaceAll("official music video", "")
                .replaceAll("official lyrics video", "")
                .replaceAll("official lyric video", "")
                .replaceAll("official video", "")
                .replaceAll("official lyrics", "")
                .replaceAll("lyrics", "")
                .replaceAll("audio", "")
                .replaceAll("music video", "")
                .replaceAll(" video", "")
                .replaceAll("karaoke version", "")
                .replaceAll("full hd", "")
                .replaceAll("hd", "")
                .replaceAll("1080p", "")
                .replaceAll("720p", "");
    }
}
