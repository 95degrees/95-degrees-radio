package me.voidinvoid.discordmusic.events;

/**
 * DiscordMusic - 30/04/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class NetworkSongException extends Exception {

    private final NetworkSongError error;

    public NetworkSongException(NetworkSongError error) {

        this.error = error;
    }

    public NetworkSongError getError() {
        return error;
    }

    @Override
    public String getMessage() {
        return error.getErrorMessage();
    }
}
