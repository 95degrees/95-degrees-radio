package me.voidinvoid.songs;

import java.io.File;

public class QuizSong extends Song {

    private File countdownLocation;

    public QuizSong(File countdownLocation) {
        super(SongType.QUIZ);
        this.countdownLocation = countdownLocation;
    }

    @Override
    public String getLocation() {
        return countdownLocation.toString();
    }

    @Override
    public String getIdentifier() {
        return countdownLocation.getPath();
    }

    @Override
    public AlbumArtType getAlbumArtType() {
        return AlbumArtType.NETWORK;
    }

    @Override
    public File getAlbumArtFile() {
        return null;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}
