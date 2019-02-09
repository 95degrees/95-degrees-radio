package me.voidinvoid.discordmusic.songs.albumart;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

import javax.annotation.CheckReturnValue;

public abstract class AlbumArt {

    public AlbumArt() {

    }

    @CheckReturnValue
    public abstract MessageAction attachAlbumArt(EmbedBuilder embed, TextChannel channel);

    @CheckReturnValue
    public abstract MessageAction attachAlbumArtToEdit(EmbedBuilder embed, Message existingMessage);
}
