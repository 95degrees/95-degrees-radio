package me.voidinvoid.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.songs.NetworkSong;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.songs.SongQueue;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

public interface SongEventListener {

    default void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
    }

    default void onSongEnd(Song song, AudioTrack track) {
    }

    default void onSongLoadError(Song song, FriendlyException error) {
    }

    default void onNoSongsInQueue(SongPlaylist playlist) {
    }

    default void onSongSeek(AudioTrack track, long seekTime, AudioPlayer player) {

    }

    default void onNetworkSongQueueError(NetworkSong song, AudioTrack track, Member member, NetworkSongError error) {
    }

    default void onNetworkSongQueued(NetworkSong song, AudioTrack track, Member member, int queuePosition) {

    }

    default void onPlaylistChange(SongPlaylist oldPlaylist, SongPlaylist newPlaylist) {
    }

    default void onSuggestionsToggle(boolean enabled, User source) {

    }
}
