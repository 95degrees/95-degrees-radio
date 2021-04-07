package me.voidinvoid.discordmusic.songs.albumart;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.commands.CommandHook;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.InteractionWebhookAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import javax.annotation.CheckReturnValue;

public abstract class AlbumArt {

    public AlbumArt() {

    }

    @CheckReturnValue
    public abstract MessageAction attachAlbumArt(EmbedBuilder embed, TextChannel channel);

    @CheckReturnValue
    public abstract MessageAction attachAlbumArtToEdit(EmbedBuilder embed, Message existingMessage);

    @CheckReturnValue
    public abstract InteractionWebhookAction attachAlbumArtToCommandHook(EmbedBuilder embed, CommandHook interactionHook);
}
