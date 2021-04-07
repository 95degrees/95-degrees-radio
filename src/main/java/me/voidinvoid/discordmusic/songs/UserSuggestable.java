package me.voidinvoid.discordmusic.songs;

import net.dv8tion.jda.api.commands.CommandHook;
import net.dv8tion.jda.api.entities.User;

/**
 * DiscordMusic - 04/08/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public interface UserSuggestable {

    boolean isSuggestion();

    User getSuggestedBy();

    CommandHook getSlashCommandSource();
}
