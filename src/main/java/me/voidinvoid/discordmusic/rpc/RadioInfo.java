package me.voidinvoid.discordmusic.rpc;

import java.util.List;

public class RadioInfo {

    public List<UserInfo> listeners;
    public SongInfo song;
    public List<SongInfo> queue;

    public RadioInfo(List<UserInfo> listeners, SongInfo song, List<SongInfo> queue) {

        this.listeners = listeners;
        this.song = song;
        this.queue = queue;
    }
}
