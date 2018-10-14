package me.voidinvoid.songs;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.utils.AlbumArtUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSong extends Song {
    static final File FALLBACK_FILE;
    static final File JINGLE_FILE;
    static final File ADVERT_FILE;

    static {
        FALLBACK_FILE = new File(RadioConfig.config.images.fallbackAlbumArt);
        JINGLE_FILE = RadioConfig.config.images.jingleAlbumArt == null ? FALLBACK_FILE : new File(RadioConfig.config.images.jingleAlbumArt);
        ADVERT_FILE = RadioConfig.config.images.advertAlbumArt == null ? FALLBACK_FILE : new File(RadioConfig.config.images.advertAlbumArt);
    }

    private File file;
    private File albumArtFile;

    public FileSong(SongType type, File file) { //todo use path api
        super(type);

        this.file = file;

        try {
            Mp3File song = new Mp3File(file);
            if (song.hasId3v2Tag()) {
                ID3v2 tag = song.getId3v2Tag();
                byte[] art = tag.getAlbumImage();

                if (art == null) return;

                String mime = tag.getAlbumImageMimeType().replace("image/", "");
                BufferedImage artImg = AlbumArtUtils.scaleAlbumArt(ImageIO.read(new ByteArrayInputStream(art)));

                Path album = Files.createTempFile("albumart-", "." + mime);
                albumArtFile = album.toFile();
                albumArtFile.deleteOnExit();

                ImageIO.write(artImg, mime, albumArtFile);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public String getLocation() {
        return file.getName();
    }

    @Override
    public String getIdentifier() {
        return file.toString();
    }

    @Override
    public AlbumArtType getAlbumArtType() {
        return AlbumArtType.FILE;
    }

    @Override
    public File getAlbumArtFile() {
        File f = getType().getAlbumArt(this);
        if (f == null) {
            f = albumArtFile == null ? FALLBACK_FILE : albumArtFile;
        }

        return f;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}
