package me.voidinvoid.utils;

import me.voidinvoid.songs.AlbumArtType;
import me.voidinvoid.songs.Song;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public final class AlbumArtUtils {

    public static final int ALBUM_ART_SCALE_SIZE = 128;

    public static MessageAction attachAlbumArt(EmbedBuilder embed, Song song, TextChannel channel) {
        if (song.getAlbumArtType() == AlbumArtType.FILE) {
            File albumArt = song.getAlbumArtFile();
            embed.setThumbnail("attachment://" + albumArt.getName());
            return channel.sendFile(albumArt).embed(embed.build());

        } else { //network album
            embed.setThumbnail(song.getAlbumArtURL());
            return channel.sendMessage(embed.build());
        }
    }

    public static MessageAction attachAlbumArtToEdit(EmbedBuilder embed, Song song, Message existingMessage) {
        if (song.getAlbumArtType() == AlbumArtType.FILE) {
            File albumArt = song.getAlbumArtFile();
            embed.setThumbnail("attachment://" + albumArt.getName());
            return existingMessage.editMessage(embed.build());

        } else { //network album
            embed.setThumbnail(song.getAlbumArtURL());
            return existingMessage.editMessage(embed.build());
        }
    }

    public static BufferedImage scaleAlbumArt(BufferedImage img) {
        BufferedImage output = new BufferedImage(ALBUM_ART_SCALE_SIZE, ALBUM_ART_SCALE_SIZE, img.getType());

        Graphics2D g2d = output.createGraphics();
        g2d.drawImage(img, 0, 0, ALBUM_ART_SCALE_SIZE, ALBUM_ART_SCALE_SIZE, null);
        g2d.dispose();

        return output;
    }
}
