package me.voidinvoid.discordmusic.songs.local;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import me.voidinvoid.discordmusic.songs.AlbumArtType;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongQueue;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.AlbumArt;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSong extends Song {

    private Path file;
    private Path albumArtFile;

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

                if (art == null) return;

                String mime = tag.getAlbumImageMimeType().replace("image/", "");
                BufferedImage artImg = AlbumArt.scaleAlbumArt(ImageIO.read(new ByteArrayInputStream(art)));

                albumArtFile = Files.createTempFile("albumart-", "." + mime);

                File af = albumArtFile.toFile();
                af.deleteOnExit();

                ImageIO.write(artImg, mime, af);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getFileName() {
        return file.getFileName().toString();
    }

    @Override
    public String getFullLocation() {
        return file.toString();
    }

    @Override
    public AlbumArtType getAlbumArtType() {
        return AlbumArtType.FILE;
    }

    @Override
    public Path getAlbumArtFile() {
        Path p = getType().getAlbumArt(this);

        //does the song type have a specific album art? if so return that
        //otherwise does it have its own album art? if so return that
        //otherwise return the 'not found' fallback album art
        return p == null ? albumArtFile == null ? AlbumArt.FALLBACK_ALBUM_ART : albumArtFile : p;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}
