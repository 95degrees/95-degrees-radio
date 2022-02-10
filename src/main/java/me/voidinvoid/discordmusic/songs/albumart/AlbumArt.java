package me.voidinvoid.discordmusic.songs.albumart;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;

import javax.annotation.CheckReturnValue;

public abstract class AlbumArt {

    public AlbumArt() {

    }

    @CheckReturnValue
    public abstract MessageAction attachAlbumArt(EmbedBuilder embed, TextChannel channel);

    @CheckReturnValue
    public abstract MessageAction attachAlbumArtToEdit(EmbedBuilder embed, Message existingMessage);

    @CheckReturnValue
    public abstract WebhookMessageUpdateAction<Message> attachAlbumArtToInteractionHook(EmbedBuilder embed, InteractionHook interactionHook);
}
