package me.voidinvoid.discordmusic.commands.slash;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface SlashCommandHandler {

    CommandData getCommand();

    default boolean requiresDjAccess() {
        return false;
    }
}
