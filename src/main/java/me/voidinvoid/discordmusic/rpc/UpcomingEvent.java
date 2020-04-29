package me.voidinvoid.discordmusic.rpc;

import java.util.concurrent.Callable;

/**
 * DiscordMusic - 10/04/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class UpcomingEvent {

    public String id;
    public String description;
    public String cancelActionName;

    private final transient Runnable cancel;

    public UpcomingEvent(String id, String description, String cancelActionName, Runnable cancel) {

        this.id = id;
        this.description = description;
        this.cancelActionName = cancelActionName;
        this.cancel = cancel;
    }

    public void cancel() {
        cancel.run();
    }
}
