package me.voidinvoid.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongPlaylist;

public interface SongEventListener {

    default void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
    }

    default void onSongEnd(Song song, AudioTrack track) {
    }

    default void onSongLoadError(Song song, FriendlyException error) {
    }

    default void onPlaylistChange(SongPlaylist oldPlaylist, SongPlaylist newPlaylist) {
    }
}
