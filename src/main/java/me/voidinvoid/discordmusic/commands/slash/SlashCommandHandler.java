package me.voidinvoid.discordmusic.commands.slash;

import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;

public interface SlashCommandHandler {

    CommandUpdateAction.CommandData getCommand();

    public default boolean requiresDjAccess() {
        return false;
    }
}
