package me.voidinvoid.quiz;

import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.songs.SongQueue;

import java.io.File;

public class QuizPlaylist extends SongPlaylist {

    public QuizPlaylist(File dir) {
        super(dir);
    }

    @Override
    public SongQueue getSongs() {
        return super.getSongs();
    }

    @Override
    public SongQueue getJingles() {
        return super.getJingles();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public boolean isDefault() {
        return super.isDefault();
    }

    @Override
    public String getInternal() {
        return super.getInternal();
    }

    @Override
    public boolean isJinglesEnabled() {
        return super.isJinglesEnabled();
    }

    @Override
    public boolean isTestingMode() {
        return super.isTestingMode();
    }

    @Override
    public void awaitLoad() {
        super.awaitLoad();
    }

    @Override
    public String getStatusOverrideMessage() {
        return super.getStatusOverrideMessage();
    }

    @Override
    public boolean isDirectMessageNotifications() {
        return super.isDirectMessageNotifications();
    }
}
