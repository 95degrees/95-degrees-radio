package me.voidinvoid.discordmusic.songs.database;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.events.RadioEventListener;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.database.triggers.TriggerActivation;

public class SongTriggerManager implements RadioService, RadioEventListener {

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (song instanceof DatabaseSong) {
            DatabaseSong ds = (DatabaseSong) song;

            ds.getTriggers().stream().filter(t -> t.getOn().equals(TriggerActivation.SONG_START)).forEachOrdered(t -> t.getType().onTrigger(song, t.getParams()));
        }
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        if (song instanceof DatabaseSong) {
            DatabaseSong ds = (DatabaseSong) song;

            ds.getTriggers().stream().filter(t -> t.getOn().equals(TriggerActivation.SONG_END)).forEachOrdered(t -> t.getType().onTrigger(song, t.getParams()));
        }
    }
}
