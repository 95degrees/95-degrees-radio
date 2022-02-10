package me.voidinvoid.discordmusic.songs;

import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.entities.User;

/**
 * DiscordMusic - 04/08/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public interface UserSuggestable {

    boolean isSuggestion();

    User getSuggestedBy();

    InteractionHook getSlashCommandSource();
}
