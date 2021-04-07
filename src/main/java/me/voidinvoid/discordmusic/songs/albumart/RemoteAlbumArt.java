package me.voidinvoid.discordmusic.songs.albumart;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.commands.CommandHook;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.InteractionWebhookAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class RemoteAlbumArt extends AlbumArt {

    private String url;

    public RemoteAlbumArt(String url) {
        super();

        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public MessageAction attachAlbumArt(EmbedBuilder embed, TextChannel channel) {
        return channel.sendMessage(embed.setThumbnail(url).build());
    }

    @Override
    public MessageAction attachAlbumArtToEdit(EmbedBuilder embed, Message existingMessage) {
        return existingMessage.editMessage(embed.setThumbnail(url).build());
    }

    @Override
    public InteractionWebhookAction attachAlbumArtToCommandHook(EmbedBuilder embed, CommandHook interactionHook) {
        return interactionHook.editOriginal(embed.setThumbnail(url).build());
    }
}
