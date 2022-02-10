package me.voidinvoid.discordmusic.songs.albumart;

import me.voidinvoid.discordmusic.Radio;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;

import javax.annotation.CheckReturnValue;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalAlbumArt extends AlbumArt {

    private Path path;

    public LocalAlbumArt(Path path) {
        super();

        this.path = path;
    }

    @CheckReturnValue
    public MessageAction attachAlbumArt(EmbedBuilder embed, TextChannel channel) {
        if (!Files.exists(path)) {
            System.out.println("Warning: invalid album art file: " + path);
            return Radio.getInstance().getService(AlbumArtManager.class).getFallbackAlbumArt().attachAlbumArt(embed, channel); //todo - could cause an infinite loop if fallback album art is invalid
        }

        embed.setThumbnail("attachment://" + path.getFileName().toString());
        return channel.sendFile(path.toFile()).embed(embed.build());
    }

    @Override
    public MessageAction attachAlbumArtToEdit(EmbedBuilder embed, Message existingMessage) {
        if (!Files.exists(path)) {
            System.out.println("Warning: invalid album art file: " + path);
            return Radio.getInstance().getService(AlbumArtManager.class).getFallbackAlbumArt().attachAlbumArtToEdit(embed, existingMessage);
        }

        embed.setThumbnail("attachment://" + path.getFileName().toString());
        return existingMessage.editMessage(embed.build());
    }

    @Override
    public WebhookMessageUpdateAction<Message> attachAlbumArtToInteractionHook(EmbedBuilder embed, InteractionHook interactionHook) {
        if (!Files.exists(path)) {
            System.out.println("Warning: invalid album art file: " + path);
            return Radio.getInstance().getService(AlbumArtManager.class).getFallbackAlbumArt().attachAlbumArtToInteractionHook(embed, interactionHook);
        }

        embed.setThumbnail("attachment://" + path.getFileName().toString());
        return interactionHook.editOriginalEmbeds(embed.build());
    }
}
