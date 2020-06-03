package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.SpotifySong;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Songs;
import org.bson.Document;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class SongRatingsCommand extends Command {

    SongRatingsCommand() {
        super("ratings", "Lists the ratings for the currently playing song or all songs", "[all]", ChannelScope.DJ_CHAT);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void invoke(CommandData data) {
        DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);

        var song = Radio.getInstance().getOrchestrator().getCurrentSong();

        if (data.getArgs().length == 0) {
            if (song == null || song.getType() != SongType.SONG) {
                data.error("No song is currently playing");
                return;
            }

            if (!(song instanceof DatabaseSong) && !(song instanceof SpotifySong)) {
                data.error("Ratings aren't available for this song");
                return;
            }

            var doc = db.getCollection("ratings").find(eq("song", song.getInternalName())).first();

            if (doc == null) {
                data.success("No ratings have been found for this song");
                return;
            }

            var ratings = doc.get("ratings", Document.class).entrySet();
            var totalRatings = ratings.stream().mapToInt(e -> ((ArrayList<String>) e.getValue()).size()).sum();

            data.code("Ratings for " + Songs.titleArtist(song) + "\n\n" + ratings.stream().map(e -> {
                double rating = ((ArrayList<String>) e.getValue()).size();
                return e.getKey() + " - " + (100d * (rating / totalRatings) + "% (" + rating + ")");
            }).collect(Collectors.joining("\n")));
            return;
        }

        if (data.getArgs()[0].equalsIgnoreCase("all")) {
            data.error("todo");
            return;
        }

        data.error("`all` required to list ratings for all songs in the current playlist");
    }
}
