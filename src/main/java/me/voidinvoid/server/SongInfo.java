package me.voidinvoid.server;

public class SongInfo {

    public String name;
    public String artist;
    public String albumArtUrl;
    public long startTime;
    public long endTime;

    public SongInfo(String name, String artist, String albumArtUrl, long startTime, long endTime) {

        this.name = name;
        this.artist = artist;
        this.albumArtUrl = albumArtUrl;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
