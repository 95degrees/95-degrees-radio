package me.voidinvoid.discordmusic.utils;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArtManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageUpdateAction;

import javax.annotation.CheckReturnValue;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class AlbumArtUtils {

    public static final int ALBUM_ART_SCALE_SIZE = 128;

    @CheckReturnValue
    public static MessageAction attachAlbumArt(EmbedBuilder embed, Song song, TextChannel channel) {

        var built = embed.build();
        if (built.getThumbnail() != null) {
            return channel.sendMessage(built);
        }

        var art = song.getAlbumArt();
        if (art == null) art = Radio.getInstance().getService(AlbumArtManager.class).getFallbackAlbumArt();

        return art.attachAlbumArt(embed, channel);
    }

    @CheckReturnValue
    public static MessageAction attachAlbumArtToEdit(EmbedBuilder embed, Song song, Message existingMessage) {
        var art = song.getAlbumArt();
        if (art == null) art = Radio.getInstance().getService(AlbumArtManager.class).getFallbackAlbumArt();

        return art.attachAlbumArtToEdit(embed, existingMessage);
    }

    @CheckReturnValue
    public static WebhookMessageUpdateAction<Message> attachAlbumArtToInteractionHook(EmbedBuilder embed, Song song, InteractionHook interactionHook) {
        var art = song.getAlbumArt();
        if (art == null) art = Radio.getInstance().getService(AlbumArtManager.class).getFallbackAlbumArt();

        return art.attachAlbumArtToInteractionHook(embed, interactionHook);
    }

    public static BufferedImage scaleAlbumArt(BufferedImage img) {
        BufferedImage output = new BufferedImage(ALBUM_ART_SCALE_SIZE, ALBUM_ART_SCALE_SIZE, img.getType());

        Graphics2D g2d = output.createGraphics();
        g2d.drawImage(img, 0, 0, ALBUM_ART_SCALE_SIZE, ALBUM_ART_SCALE_SIZE, null);
        g2d.dispose();

        return output;
    }
}
