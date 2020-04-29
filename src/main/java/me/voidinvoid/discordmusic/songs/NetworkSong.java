package me.voidinvoid.discordmusic.songs;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.cache.YouTubeCacheManager;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArt;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArtManager;
import me.voidinvoid.discordmusic.songs.albumart.RemoteAlbumArt;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.entities.User;

public class NetworkSong extends Song {

    private String url;
    private User suggestedBy;
    private AudioTrack track;

    private AlbumArt albumArt;

    public NetworkSong(SongType type, AudioTrack track, User suggestedBy) {
        super(type);

        this.url = track.getInfo().uri;
        this.suggestedBy = suggestedBy;
        this.track = track;
        track.setUserData(this);

        if (track instanceof YoutubeAudioTrack) { //fetch youtube album art
            albumArt = new RemoteAlbumArt("https://img.youtube.com/vi/" + track.getIdentifier().split("\\?v=")[0] + "/mqdefault.jpg");
        }
    }

    @Override
    public String getFriendlyName() {
        return track.getInfo().title + " (" + track.getInfo().author + ")";
    }

    @Override
    public String getFileName() {
        return track.getInfo().uri;
    }

    @Override
    public AudioTrack getTrack() {
        track = track.makeClone();
        track.setUserData(this);
        return track;
    }

    @Override
    public String getFullLocation() {
        return url;
    }

    @Override
    public AlbumArt getAlbumArt() {
        var p = getType().getAlbumArt(this);

        //if this song type overrides album art, use that. otherwise, use our own album art
        return p == null ? albumArt == null ? Service.of(AlbumArtManager.class).getNetworkAlbumArt() : albumArt : p;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }

    public User getSuggestedBy() {
        return suggestedBy;
    }
}
