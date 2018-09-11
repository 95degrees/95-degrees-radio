package me.voidinvoid.status;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.Radio;
import me.voidinvoid.events.SongEventListener;
import me.voidinvoid.songs.NetworkSong;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Game;

import java.util.HashMap;
import java.util.Map;

public class StatusManager implements SongEventListener {

    private Map<Song, String> songStatusOverrides = new HashMap<>();

    private JDA jda;

    public StatusManager(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        String statusOverride = songStatusOverrides.remove(song);

        if (statusOverride != null) { //normally for special task songs
            jda.getPresence().setGame(Game.listening(statusOverride));

        } else if ((statusOverride = Radio.instance.getOrchestrator().getActivePlaylist().getStatusOverrideMessage()) != null) { //normally for scheduled playlists
            jda.getPresence().setGame(Game.playing(statusOverride));

        } else if (song.getType() == SongType.SONG) {
            if (track.getInfo().isStream) {
                jda.getPresence().setGame(Game.streaming(track.getInfo().title, track.getInfo().uri));
            } else {
                jda.getPresence().setGame(Game.listening(song instanceof NetworkSong ? track.getInfo().title : (track.getInfo().author + " - " + track.getInfo().title)));
            }
        }
    }

    public void addSongOverride(Song song, String message) {
        songStatusOverrides.put(song, message);
    }
}
