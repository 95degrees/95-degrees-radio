package me.voidinvoid.discordmusic.utils;

import me.voidinvoid.discordmusic.songs.AlbumArtType;
import me.voidinvoid.discordmusic.songs.Song;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

import javax.annotation.CheckReturnValue;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class AlbumArtUtils {

    public static final int ALBUM_ART_SCALE_SIZE = 128;

    @CheckReturnValue
    public static MessageAction attachAlbumArt(EmbedBuilder embed, Song song, TextChannel channel, boolean forceLocal) {
        if (forceLocal || song.getAlbumArtType() == AlbumArtType.FILE) {
            File albumArt = song.getAlbumArtFile();
            embed.setThumbnail("attachment://" + albumArt.getName());
            return channel.sendFile(albumArt).embed(embed.build());

        } else { //network album
            embed.setThumbnail(song.getAlbumArtURL());
            return channel.sendMessage(embed.build());
        }
    }

    @CheckReturnValue
    public static MessageAction attachAlbumArt(EmbedBuilder embed, Song song, TextChannel channel) {
        return attachAlbumArt(embed, song, channel, false);
    }

    @CheckReturnValue
    public static MessageAction attachAlbumArtToEdit(EmbedBuilder embed, Song song, Message existingMessage, boolean forceLocal) {
        if (forceLocal || song.getAlbumArtType() == AlbumArtType.FILE) {
            File albumArt = song.getAlbumArtFile();
            embed.setThumbnail("attachment://" + albumArt.getName());
            return existingMessage.editMessage(embed.build());

        } else { //network album
            embed.setThumbnail(song.getAlbumArtURL());
            return existingMessage.editMessage(embed.build());
        }
    }

    @CheckReturnValue
    public static MessageAction attachAlbumArtToEdit(EmbedBuilder embed, Song song, Message existingMessage) {
        return attachAlbumArtToEdit(embed, song, existingMessage, false);
    }

    public static BufferedImage scaleAlbumArt(BufferedImage img) {
        BufferedImage output = new BufferedImage(ALBUM_ART_SCALE_SIZE, ALBUM_ART_SCALE_SIZE, img.getType());

        Graphics2D g2d = output.createGraphics();
        g2d.drawImage(img, 0, 0, ALBUM_ART_SCALE_SIZE, ALBUM_ART_SCALE_SIZE, null);
        g2d.dispose();

        return output;
    }
}
