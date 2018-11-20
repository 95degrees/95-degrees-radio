package me.voidinvoid.discordmusic.server;

import java.util.List;

public class RadioInfo {

    public List<MemberInfo> listeners;
    public List<MemberInfo> members;
    public SongInfo song;

    public RadioInfo(List<MemberInfo> listeners, List<MemberInfo> members, SongInfo song) {

        this.listeners = listeners;
        this.members = members;
        this.song = song;
    }
}
