package me.voidinvoid.discordmusic.events;

import me.voidinvoid.discordmusic.SongOrchestrator;

public enum NetworkSongError {

    EXCEEDS_LENGTH_LIMIT("Song is too long to be added to the queue"),
    QUEUE_LIMIT_REACHED("Song could not be queued because you already have at least " + SongOrchestrator.USER_QUEUE_LIMIT + " suggested songs in the queue"),
    IS_STREAM("Streams cannot be queued"),
    SONG_SUGGESTIONS_DISABLED("Song suggestions are currently disabled"),
    NOT_IN_VOICE_CHANNEL("You must be in the radio voice channel to make song suggestions");

    private String errorMessage;

    NetworkSongError(String errorMessage) {

        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
