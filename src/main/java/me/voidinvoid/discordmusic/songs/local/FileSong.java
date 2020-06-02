package me.voidinvoid.discordmusic.songs.local;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongQueue;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArt;
import me.voidinvoid.discordmusic.songs.albumart.LocalAlbumArt;
import me.voidinvoid.discordmusic.utils.AlbumArtUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSong extends Song {

    private Path file;
    private AlbumArt albumArt;

    private String mp3Title, mp3Artist;

    public FileSong(SongType type, Path file) {
        this(type, file, null);
    }

    public FileSong(SongType type, Path file, SongQueue songQueue) {
        super(type);

        this.file = file;
        this.setQueue(songQueue);

        try {
            Mp3File song = new Mp3File(file);
            if (song.hasId3v2Tag()) {
                ID3v2 tag = song.getId3v2Tag();
                byte[] art = tag.getAlbumImage();

                mp3Title = tag.getTitle();
                mp3Artist = tag.getArtist();

                if (art == null) return;

                String mime = tag.getAlbumImageMimeType().replace("image/", "");
                BufferedImage artImg = AlbumArtUtils.scaleAlbumArt(ImageIO.read(new ByteArrayInputStream(art)));

                var ap = Files.createTempFile("albumart-", "." + mime);

                albumArt = new LocalAlbumArt(ap);

                File af = ap.toFile();
                af.deleteOnExit();

                ImageIO.write(artImg, mime, af);
            }
        } catch (Exception ignored) {
        }
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
        return true;
    }
}
