package me.voidinvoid.discordmusic.songs.local;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongQueue;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.UserSuggestable;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArt;
import me.voidinvoid.discordmusic.songs.albumart.LocalAlbumArt;
import me.voidinvoid.discordmusic.utils.AlbumArtUtils;
import me.voidinvoid.discordmusic.utils.cache.CachedUser;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.entities.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSong extends Song implements UserSuggestable {

    private Path file;
    private CachedUser suggestedBy;
    private AlbumArt albumArt;
    private InteractionHook slashCommandSource;

    private AudioTrack track;

    private String mp3Title, mp3Artist;

    public FileSong(SongType type, Path file, User suggestedBy) {
        this(type, file, suggestedBy, null, null);
    }

    public FileSong(SongType type, Path file, User suggestedBy, SongQueue songQueue, InteractionHook slashCommandSource) {
        super(type);

        this.file = file;
        this.suggestedBy = suggestedBy == null ? null : new CachedUser(suggestedBy);
        this.setQueue(songQueue);
        this.slashCommandSource = slashCommandSource;

        try {
            Mp3File song = new Mp3File(file);
            if (song.hasId3v2Tag()) {
                ID3v2 tag = song.getId3v2Tag();
                byte[] art = tag.getAlbumImage();

                mp3Title = tag.getTitle();
                mp3Artist = tag.getArtist();

                if (art != null) {
                    String mime = tag.getAlbumImageMimeType().replace("image/", "");
                    BufferedImage artImg = AlbumArtUtils.scaleAlbumArt(ImageIO.read(new ByteArrayInputStream(art)));

                    var ap = Files.createTempFile("albumart-", "." + mime);

                    albumArt = new LocalAlbumArt(ap);

                    File af = ap.toFile();
                    af.deleteOnExit();

                    ImageIO.write(artImg, mime, af);
                }
            }
        } catch (Exception ignored) {
        }

        if (mp3Title == null) {
            mp3Title = "Untitled upload";
        }

        if (mp3Artist == null) {
            mp3Artist = "Unknown artist";
        }
    }

    @Override
    public AudioTrack getTrack() {
        return track;
    }

    public FileSong setTrack(AudioTrack track) {
        this.track = track;
        return this;
    }

    @Override
    public String getTitle() {
        return mp3Title;
    }

    @Override
    public String getArtist() {
        return mp3Artist;
    }

    @Override
    public String getInternalName() {
        return file.getFileName().toString();
    }

    @Override
    public String getLavaIdentifier() {
        return file.toString();
    }

    @Override
    public AlbumArt getAlbumArt() {
        var p = getType().getAlbumArt(this);

        //if this song type overrides album art, use that. otherwise, use our own album art
        return p == null ? albumArt : p;
    }

    @Override
    public boolean isPersistent() {
        return suggestedBy == null;
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
