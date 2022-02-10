package me.voidinvoid.discordmusic.songs;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandData;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArt;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArtManager;
import me.voidinvoid.discordmusic.songs.albumart.RemoteAlbumArt;
import me.voidinvoid.discordmusic.utils.Service;
import me.voidinvoid.discordmusic.utils.cache.CachedUser;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.entities.User;

public class NetworkSong extends Song implements SpotifyTrackHolder, UserSuggestable {

    private String url;
    private CachedUser suggestedBy;
    private AudioTrack track;
    private Track spotifyTrack;
    private InteractionHook slashCommandSource;

    private AlbumArt albumArt;

    public NetworkSong(SongType type, AudioTrack track, User suggestedBy, InteractionHook slashCommandSource) {
        super(type);

        this.url = track.getInfo().uri;
        this.suggestedBy = suggestedBy == null ? null : new CachedUser(suggestedBy);
        this.slashCommandSource = slashCommandSource;
        this.track = track;
        track.setUserData(this);

        if (track instanceof YoutubeAudioTrack) { //fetch youtube album art
            albumArt = new RemoteAlbumArt("https://img.youtube.com/vi/" + track.getIdentifier().split("\\?v=")[0] + "/mqdefault.jpg");
        }
    }

    @Override
    public String getTitle() {
        return spotifyTrack == null ? track.getInfo().title : spotifyTrack.getName();
    }

    @Override
    public String getArtist() {
        return spotifyTrack == null || spotifyTrack.getArtists().length == 0 ? track.getInfo().author : spotifyTrack.getArtists()[0].getName();
    }

    @Override
    public String getInternalName() {
        return track.getInfo().uri;
    }

    @Override
    public AudioTrack getTrack() {
        track = track.makeClone();
        track.setUserData(this);
        return track;
    }

    @Override
    public String getLavaIdentifier() {
        return url;
    }

    @Override
    public AlbumArt getAlbumArt() {
        //if this song type overrides album art, use that. otherwise, use our own album art
        var p = getType().getAlbumArt(this);

        if (p != null) {
            return p;
        }

        if (albumArt != null) {
            return albumArt;
        }

        return Service.of(AlbumArtManager.class).getNetworkAlbumArt();
    }

    @Override
    public Track getSpotifyTrack() {
        return spotifyTrack;
    }

    public void setSpotifyTrack(Track spotifyTrack) {
        this.spotifyTrack = spotifyTrack;

        if (spotifyTrack != null && spotifyTrack.getAlbum().getImages() != null && spotifyTrack.getAlbum().getImages().length > 0) {
            albumArt = new RemoteAlbumArt(spotifyTrack.getAlbum().getImages()[0].getUrl());
        }
    }

    @Override
    public boolean isPersistent() {
        return !isSuggestion();
    }

    @Override
    public boolean isSuggestion() {
        return suggestedBy != null;
    }

    @Override
    public User getSuggestedBy() {
        return suggestedBy == null ? null : suggestedBy.get();
    }

    @Override
    public InteractionHook getSlashCommandSource() {
        return slashCommandSource;
    }
}
