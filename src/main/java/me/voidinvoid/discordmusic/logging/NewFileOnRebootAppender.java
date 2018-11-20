package me.voidinvoid.discordmusic.logging;

import org.apache.log4j.FileAppender;

public class NewFileOnRebootAppender extends FileAppender {

    public NewFileOnRebootAppender() {
    }

    @Override
    public void setFile(String file) {
        super.setFile(prependDate(file));
    }

    private static String prependDate(String filename) {
        return "logs/" + System.currentTimeMillis() + "_" + filename;
    }
}