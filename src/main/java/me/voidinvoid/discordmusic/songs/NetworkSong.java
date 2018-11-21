package me.voidinvoid.discordmusic.songs;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.config.RadioConfig;
import net.dv8tion.jda.core.entities.User;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NetworkSong extends Song {

    public static final Path NETWORK_ALBUM_ART;

    static {
        NETWORK_ALBUM_ART = Paths.get(RadioConfig.config.images.networkAlbumArt);
    }

    private String url;
    private User suggestedBy;
    private AudioTrack track;

    private String albumArtUrl;

    public NetworkSong(SongType type, AudioTrack track, User suggestedBy) {
        super(type);

        this.url = track.getInfo().uri;
        this.suggestedBy = suggestedBy;
        this.track = track;
        track.setUserData(this);

        if (track instanceof YoutubeAudioTrack) { //fetch youtube album art
            albumArtUrl = "https://img.youtube.com/vi/" + track.getIdentifier().split("\\?v=")[0] + "/mqdefault.jpg";
        }
    }

    @Override
    public String getFileName() {
        return track.getInfo().uri;
    }

    @Override
    public AudioTrack getTrack() {
        return track;
    }

    @Override
    public String getFullLocation() {
        return url;
    }

    @Override
    public AlbumArtType getAlbumArtType() {
        return albumArtUrl == null ? AlbumArtType.FILE : AlbumArtType.NETWORK;
    }

    @Override
    public Path getAlbumArtFile() {
        return NETWORK_ALBUM_ART;
    }

    @Override
    public String getAlbumArtURL() {
        return albumArtUrl;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    public User getSuggestedBy() {
        return suggestedBy;
    }
}
