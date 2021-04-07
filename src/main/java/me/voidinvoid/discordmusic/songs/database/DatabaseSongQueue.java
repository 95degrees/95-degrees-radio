package me.voidinvoid.discordmusic.songs.database;

import com.wrapper.spotify.model_objects.specification.Track;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.*;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import me.voidinvoid.discordmusic.utils.Service;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
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
                    String sourceName = d.getString("source");
                    DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);
                    Document source = db.getCollection("sources").find(eq("_id", sourceName)).first();

                    if (source == null) {
                        System.out.println("Unknown source '" + sourceName + "'");
                        return Stream.empty();
                    } else {
                        List<Document> listing = (ArrayList<Document>) source.get("listing");
                        return listing.stream().map(v -> new DatabaseSong(getQueueType(), v, sourceName).setQueue(this));
                    }
                } else if ("SPOTIFY".equals(type)) {
                    String playlistName = d.getString("playlist");
                    var sm = Service.of(SpotifyManager.class);

                    try {
                        List<Song> tracks = new ArrayList<>();

                        boolean moreTracks;
                        int offset = 0;

                        do {
                            var spotifyPlaylist = sm.getSpotifyApi().getPlaylistsItems(playlistName).offset(offset).limit(100).build().execute();

                            for (var track : spotifyPlaylist.getItems()) {
                                tracks.add(new SpotifySong(getQueueType(), (Track) track.getTrack()).setQueue(this));
                            }

                            moreTracks = spotifyPlaylist.getNext() != null;
                            offset += 100;

                        } while (moreTracks);

                        return tracks.stream();
                    } catch (Exception ex) {
                        System.out.println("Error loading songs from Spotify playlist");
                        ex.printStackTrace();
                        return Stream.empty();
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
