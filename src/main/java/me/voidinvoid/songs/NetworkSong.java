package me.voidinvoid.songs;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.RadioConfig;
import net.dv8tion.jda.core.entities.User;

import java.io.File;

public class NetworkSong extends Song {
    public static final File NETWORK_ALBUM;

    //public static final Pattern YOUTUBE_MATCHER = Pattern.compile("(youtu\\.be/|youtube\\.com/(watch\\?(.*&)?v=|(embed|v)/))([^?&\"'>]+)");

    static {
        NETWORK_ALBUM = new File(RadioConfig.config.images.networkAlbumArt);
    }

    private String url;
    private User suggestedBy;
    private AudioTrack track;

    private String albumArtUrl;

    public NetworkSong(AudioTrack track, User suggestedBy, boolean isJingle) {
        super(isJingle);
        this.url = track.getInfo().uri;
        this.suggestedBy = suggestedBy;
        this.track = track;
        track.setUserData(this);

        if (track instanceof YoutubeAudioTrack) {
            albumArtUrl = "https://img.youtube.com/vi/" + track.getIdentifier().split("\\?v=")[0] + "/mqdefault.jpg";
        }
    }

    @Override
    public String getLocation() {
        return track.getInfo().uri;
    }

    @Override
    public AudioTrack getTrack() {
        return track;
    }

    @Override
    public String getIdentifier() {
        return url;
    }

    @Override
    public AlbumArtType getAlbumArtType() {
        return albumArtUrl == null ? AlbumArtType.FILE : AlbumArtType.NETWORK;
    }

    @Override
    public File getAlbumArtFile() {
        return NETWORK_ALBUM;
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
