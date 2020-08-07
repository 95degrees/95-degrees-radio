package me.voidinvoid.discordmusic.status;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.RadioEventListener;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.UserSuggestable;
import me.voidinvoid.discordmusic.utils.Songs;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

import java.util.HashMap;
import java.util.Map;

public class StatusManager implements RadioService, RadioEventListener {

    private Map<Song, String> songStatusOverrides = new HashMap<>();

    private JDA jda;

    @Override
    public boolean canRun(RadioConfig config) {
        return config.useStatus;
    }

    @Override
    public void onLoad() {
        jda = Radio.getInstance().getJda();
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        String statusOverride = songStatusOverrides.remove(song);

        if (statusOverride != null) { //normally for special task songs
            jda.getPresence().setActivity(Activity.playing(statusOverride));

        } else if ((statusOverride = Radio.getInstance().getOrchestrator().getActivePlaylist().getStatusOverrideMessage()) != null) { //normally for scheduled playlists
            jda.getPresence().setActivity(Activity.playing(statusOverride));

        } else if (song.getType().usesStatus()) {
            if (track.getInfo().isStream) {
                jda.getPresence().setActivity(Activity.streaming(track.getInfo().title, track.getInfo().uri));
            } else {
                jda.getPresence().setActivity(Activity.listening(song instanceof UserSuggestable ? song.getTitle() : Songs.titleArtist(song)));
            }
        }
    }

    @Override
    public void onTrackStopped() {
        if (Radio.getInstance().getOrchestrator().getActivePlaylist().getStatusOverrideMessage() == null) { //normally for scheduled playlists
            jda.getPresence().setActivity(null);
        }
    }

    public void addSongOverride(Song song, String message) {
        songStatusOverrides.put(song, message);
    }
}
