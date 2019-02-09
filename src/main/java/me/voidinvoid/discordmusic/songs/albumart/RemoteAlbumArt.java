package me.voidinvoid.discordmusic.songs.albumart;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

public class RemoteAlbumArt extends AlbumArt {

    private String url;

    public RemoteAlbumArt(String url) {
        super();

        this.url = url;
    }

    @Override
    public MessageAction attachAlbumArt(EmbedBuilder embed, TextChannel channel) {
        return channel.sendMessage(embed.setThumbnail(url).build());
    }

    @Override
    public MessageAction attachAlbumArtToEdit(EmbedBuilder embed, Message existingMessage) {
        return existingMessage.editMessage(embed.setThumbnail(url).build());
    }
}
