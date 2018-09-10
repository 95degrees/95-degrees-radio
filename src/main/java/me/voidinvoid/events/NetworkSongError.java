package me.voidinvoid.events;

import me.voidinvoid.SongOrchestrator;

public enum NetworkSongError {

    EXCEEDS_LENGTH_LIMIT("Song is too long to be added to the queue"),
    QUEUE_LIMIT_REACHED("Song could not be queued because you already have at least " + SongOrchestrator.USER_QUEUE_LIMIT + " suggested songs in the queue"),
    IS_STREAM("Streams cannot be queued"),
    SONG_SUGGESTIONS_DISABLED("Song suggestions are currently disabled");

    private String errorMessage;

    NetworkSongError(String errorMessage) {

        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
