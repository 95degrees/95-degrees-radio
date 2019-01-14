package me.voidinvoid.discordmusic.songs.database;

import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongQueue;
import me.voidinvoid.discordmusic.songs.SongType;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;

public class DatabaseSongQueue extends SongQueue {

    private List<Document> listing;

    public DatabaseSongQueue(List<Document> listing, Playlist playlist, SongType queueType, boolean shuffleSongs) {
        super(playlist, queueType, shuffleSongs);

        this.listing = listing;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Song> initSongs() {
        try {
            return listing.stream().flatMap(d -> {
                String type = d.getString("type");
                if ("SONG".equals(type)) {
                    return Stream.of(new DatabaseSong(getQueueType(), d).setQueue(this));
                } else if ("SOURCE".equals(type)) {
                    System.out.println("FOUND SOURCE: " + d.toJson());
                    String sourceName = d.getString("source");
                    DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);
                    Document source = db.getCollection("sources").find(eq("_id", sourceName)).first();

                    if (source == null) {
                        System.out.println("Unknown source '" + sourceName + "'");
                        return Stream.empty();
                    } else {
                        List<Document> listing = (ArrayList<Document>) source.get("listing"); //todo check
                        return listing.stream().map(v -> new DatabaseSong(getQueueType(), v, sourceName).setQueue(this));
                    }
                } else {
                    System.out.println("Unknown song listing type '" + type + "'");
                    return Stream.empty();
                }
            }).collect(Collectors.toList());

        } catch (Exception ex) {
            System.err.println("Error fetching songs for database playlist");
            ex.printStackTrace();

            return null;
        }
    }
}
