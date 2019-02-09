package me.voidinvoid.discordmusic;

import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.ConsoleColor;

public interface RadioService {

    default boolean canRun(RadioConfig config) {
        return true;
    }

    default String getLogPrefix() {
        return ConsoleColor.WHITE_BACKGROUND_BRIGHT + " ? ";
    }

    default void log(Object msg) {
        log(getLogPrefix() + ConsoleColor.RESET_SPACE + msg);
    }

    default void onLoad() {
    }

    default void onShutdown() {
    }
}
