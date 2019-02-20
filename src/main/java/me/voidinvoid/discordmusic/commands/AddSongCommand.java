package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.utils.AlbumArtUtils;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import org.bson.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class AddSongCommand extends Command {

    AddSongCommand() {
        super("add-song", "Adds a song to a radio playlist", "<playlist|source> <playlist/source name> <url> <title>;<artist>;<album art>[;<mbid>]", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        if (data.getArgs().length < 1) {
            data.error("'playlist' or 'source' required");
            return;
        }

        var mode = data.getArgs()[0].toLowerCase();

        if (!mode.equals("playlist") && !mode.equals("source")) {
            data.error("'playlist' or 'source' required");
            return;
        }


        if (data.getArgs().length < 2) {
            data.error("Playlist ID required");
            return;
        }

        var playlistName = data.getArgs()[1];

        if (mode.equals("playlist") && Radio.getInstance().getOrchestrator().getPlaylists().stream().noneMatch(p -> p instanceof RadioPlaylist && p.getInternal().equals(playlistName))) {
            data.error("Unknown playlist specified");
            return;
        }

        if (data.getArgs().length < 3) {
            data.error("Song URL required");
            return;
        }

        var url = data.getArgs()[2];

        if (data.getArgs().length < 4) {
            data.error("Song title required");
            return;
        }

        var meta = data.getArgsString().split(" ", 4)[3].split(";");

        var title = meta[0];

        if (meta.length < 2) {
            data.error("Song artist required");
            return;
        }

        var artist = meta[1];

        if (meta.length < 3) {
            data.error("Album art image URL required");
            return;
        }

        BufferedImage image = null;
        try {
            image = ImageIO.read(new URL(meta[2]));
                    //"https://coverartarchive.org/release-group/" + mbId + "/front"));
        } catch (IOException e) {
            e.printStackTrace();
            data.success("Error finding album art for this song - it will be blank");
        }

        var mbId = meta.length < 4 ? null : meta[3];

        var id = UUID.randomUUID().toString();

        if (image != null) {
            image = AlbumArtUtils.scaleAlbumArt(image);

            String loc = "/home/discord/radio_old/AlbumArt/Songs/" + id + ".png";
            try {
                ImageIO.write(image, "png", new File(loc));
            } catch (IOException e) {
                data.success("Error writing image!");
                e.printStackTrace();
            }
        }

        var doc = new Document("type", "SONG").append("title", title).append("artist", artist).append("mbId", mbId).append("source", url).append("albumArt", id);

        var db = Radio.getInstance().getService(DatabaseManager.class);

        db.getCollection(mode + "s").updateOne(eq(playlistName), new Document("$addToSet", new Document(mode.equals("source") ? "listing" : "listing.songs", doc)));

        Radio.getInstance().getOrchestrator().loadPlaylists();

        data.success("Inserted song (" + id + ") and reloaded playlists");
    }
}
