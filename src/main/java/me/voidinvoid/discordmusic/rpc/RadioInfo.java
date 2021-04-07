package me.voidinvoid.discordmusic.rpc;

import java.util.List;

public class RadioInfo {

    public String inviteLink;
    public List<UserInfo> listeners;
    public SongInfo song;
    public List<SongInfo> queue;
    public List<UpcomingEvent> events;
    public boolean paused;
    public int volume;

    public RadioInfo(String inviteLink, List<UserInfo> listeners, SongInfo song, List<SongInfo> queue, List<UpcomingEvent> events, boolean paused, int volume) {

        this.inviteLink = inviteLink;
        this.listeners = listeners;
        this.song = song;
        this.queue = queue;
        this.events = events;
        this.paused = paused;
        this.volume = volume;
    }
}
