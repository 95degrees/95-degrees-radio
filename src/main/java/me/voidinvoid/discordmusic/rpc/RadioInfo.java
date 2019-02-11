package me.voidinvoid.discordmusic.rpc;

import java.util.List;

public class RadioInfo {

    public List<UserInfo> listeners;
    public List<UserInfo> members;
    public SongInfo song;

    public RadioInfo(List<UserInfo> listeners, List<UserInfo> members, SongInfo song) {

        this.listeners = listeners;
        this.members = members;
        this.song = song;
    }
}
