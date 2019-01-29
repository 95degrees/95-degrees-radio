package me.voidinvoid.discordmusic.status;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Game;

import java.util.HashMap;
import java.util.Map;

public class StatusManager implements RadioService, SongEventListener {

    private Map<Song, String> songStatusOverrides = new HashMap<>();

    private JDA jda;

    @Override
    public void onLoad() {
        jda = Radio.getInstance().getJda();
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        String statusOverride = songStatusOverrides.remove(song);

        if (statusOverride != null) { //normally for special task songs
            jda.getPresence().setGame(Game.playing(statusOverride));

        } else if ((statusOverride = Radio.getInstance().getOrchestrator().getActivePlaylist().getStatusOverrideMessage()) != null) { //normally for scheduled playlists
            jda.getPresence().setGame(Game.playing(statusOverride));

        } else if (song.getType().usesStatus()) {
            if (track.getInfo().isStream) {
                jda.getPresence().setGame(Game.streaming(track.getInfo().title, track.getInfo().uri));
            } else {
                jda.getPresence().setGame(Game.playing(song instanceof DatabaseSong ? ((DatabaseSong) song).getArtist() + " - " + ((DatabaseSong) song).getTitle() : song instanceof NetworkSong ? track.getInfo().title : (track.getInfo().author + " - " + track.getInfo().title)));
            }
        }
    }

    @Override
    public void onTrackStopped() {
        if (Radio.getInstance().getOrchestrator().getActivePlaylist().getStatusOverrideMessage() == null) { //normally for scheduled playlists
            jda.getPresence().setGame(null);

        }

    }

    public void addSongOverride(Song song, String message) {
        songStatusOverrides.put(song, message);
    }
}
