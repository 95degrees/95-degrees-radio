package me.voidinvoid.discordmusic;

import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.ConsoleColor;

public interface RadioService {

    default boolean canRun(RadioConfig config) {
        return true;
    }

    default String getLogPrefix() {
        return ConsoleColor.BLACK_BACKGROUND_BRIGHT + " " + getClass().getSimpleName() + " ";
    }

    default void log(Object msg) {
        System.out.println(getLogPrefix() + ConsoleColor.RESET_SPACE + msg);
    }

    default void onLoad() {
    }

    default void onShutdown() {
    }
}
