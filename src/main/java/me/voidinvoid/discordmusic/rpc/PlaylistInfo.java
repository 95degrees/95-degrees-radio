package me.voidinvoid.discordmusic.rpc;

import me.voidinvoid.discordmusic.songs.RadioPlaylist;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class PlaylistInfo {

    public String name;
    public String id;

    public PlaylistInfo(RadioPlaylist playlist) {

        name = playlist.getName();
        id = playlist.getInternal();
    }
}
