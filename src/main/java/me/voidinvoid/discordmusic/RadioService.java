package me.voidinvoid.discordmusic;

import me.voidinvoid.discordmusic.utils.ConsoleColor;

public interface RadioService { //TODO radio service lifecycle hooks, reload, shutdown etc., and prefix support built in

    default String getLogPrefix() {
        return ConsoleColor.WHITE_BACKGROUND_BRIGHT + " ? ";
    }

    default void log(Object msg) {
        System.out.println(getLogPrefix() + ConsoleColor.RESET_SPACE + msg);
    }

    default void onLoad() {
    }

    default void onShutdown() {
    }
}
