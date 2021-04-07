package me.voidinvoid.discordmusic;

import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.ConsoleColor;

import java.time.OffsetDateTime;

public interface RadioService {

    default boolean canRun(RadioConfig config) {
        return true;
    }

    default String getLogPrefix() {
        return ConsoleColor.BLACK_BACKGROUND_BRIGHT + " " + getClass().getSimpleName() + " ";
    }

    default void log(Object msg) {
        System.out.println(ConsoleColor.WHITE + ConsoleColor.FORMATTER.format(OffsetDateTime.now()) + ConsoleColor.RESET_SPACE + getLogPrefix() + ConsoleColor.RESET_SPACE + msg);
    }

    default void warn(Object msg) {
        System.out.println(getLogPrefix() + ConsoleColor.RESET_SPACE + ConsoleColor.YELLOW_BRIGHT + msg + ConsoleColor.RESET);
    }

    default void onLoad() {
    }

    default void onShutdown() {
    }
}
