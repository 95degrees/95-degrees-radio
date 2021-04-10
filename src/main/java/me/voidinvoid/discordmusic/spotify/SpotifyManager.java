package me.voidinvoid.discordmusic.spotify;

import com.google.gson.Gson;
import com.mongodb.client.model.UpdateOptions;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Track;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.Service;
import me.voidinvoid.discordmusic.utils.Songs;
import org.bson.Document;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * DiscordMusic - 30/04/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class SpotifyManager implements RadioService {

    public static final String SPOTIFY_TRACK_URL = "https://open.spotify.com/track/";
    private static final String SPOTIFY_TRACK_URL_REGEX = "^https?://open.spotify.com/track/[a-zA-Z0-9]{22}$";
    public static final Pattern SPOTIFY_TRACK_URL_PATTERN = Pattern.compile(SPOTIFY_TRACK_URL_REGEX);

    private SpotifyApi spotifyApi;

    private OffsetDateTime lastCollaborativePlaylistCheck = OffsetDateTime.now();

    private ScheduledExecutorService executorService;

    @Override
    public void onLoad() {

        executorService = Executors.newScheduledThreadPool(1);

        var res = authenticate();

        if (res == null) {
            return;
        }

        executorService.scheduleWithFixedDelay(() -> {

            try {
                var playlist = spotifyApi.getPlaylist("0yMCH3oKa943HwWCqEUVft").build().execute(); //todo no hardcode

                if (playlist == null) return;

                var tracks = playlist.getTracks().getItems();
                var now = lastCollaborativePlaylistCheck.toInstant();
                lastCollaborativePlaylistCheck = OffsetDateTime.now();

                for (var track : tracks) {
                    if (track.getAddedAt().toInstant().isAfter(now)) {
                        log("found track, queueing!!!");
                        var spotifyTrack = (Track) track.getTrack();
                        fetchLavaTrack(spotifyTrack).thenAccept(s -> {
                            var song = new NetworkSong(SongType.SONG, s, Radio.getInstance().getJda().getSelfUser(), null);
                            song.setSpotifyTrack(spotifyTrack);
                            Radio.getInstance().getOrchestrator().queueSuggestion(song);
                        });
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }, 10, 3, TimeUnit.SECONDS);
    }

    public ClientCredentials authenticate() {
        try {
            spotifyApi = new SpotifyApi.Builder()
                    .setClientId("a8914a22ae464d6bae3539eecb20d19c")
                    .setClientSecret("25dc603108814cddb7859eaf73b71cdb")
                    .setRedirectUri(new URI("http://localhost/")) //todo store in config
                    .build();

            var credentials = spotifyApi.clientCredentials().build();
            var res = credentials.execute();

            log("Spotify API credentials: " + new Gson().toJson(res));

            spotifyApi.setAccessToken(res.getAccessToken());

            executorService.schedule(this::authenticate, res.getExpiresIn() - 30, TimeUnit.SECONDS);
            //reauthenticate automatically

            return res;

        } catch (Exception ex) {
            log("Error loading Spotify API:");
            ex.printStackTrace();
        }

        return null;
    }

    public String getIdentifier(Track track) {
        var cached = getCachedIdentifier(track);

        if (cached != null) {
            return cached;
        }

        var lava = fetchLavaTrack(track).join();

        if (lava == null) {
            return null;
        }

        return lava.getIdentifier();
    }

    public String getCachedIdentifier(Track track) {
        var coll = Service.of(DatabaseManager.class).getCollection("spotifycache");
        var doc = coll.find(eq(track.getId())).first();

        if (doc != null) {
            return doc.getString("identifier");
        }

        return null;
    }

    public void saveCachedIdentifier(Track track, String identifier) {
        var coll = Service.of(DatabaseManager.class).getCollection("spotifycache");

        coll.updateOne(eq(track.getId()), new Document("$set", new Document("identifier", identifier).append("title", track.getName()).append("cached_at", System.currentTimeMillis())), new UpdateOptions().upsert(true));
    }

    @Override
    public void onShutdown() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    public CompletableFuture<Track> searchTrack(String title) {
        var future = new CompletableFuture<Track>();

        title = Songs.deyoutubeifySong(title);

        log("search: " + title);
        spotifyApi.searchTracks(title).build().executeAsync()
                .whenComplete((t, e) -> {
                    log("search complete! " + t.getItems().length);
                    if (t.getItems().length > 0) {
                        log("item 0: " + t.getItems()[0].getName());
                    }

                    if (e != null) {
                        e.printStackTrace();
                        future.complete(null);
                        return;
                    }

                    if (t.getItems().length == 0) {
                        future.complete(null);
                        return;
                    }

                    future.complete(t.getItems()[0]);
                });

        return future;
    }

    public SpotifyApi getSpotifyApi() {
        return spotifyApi;
    }

    public void findPlaylist(String playlist) {
        spotifyApi.getPlaylist(playlist).build().executeAsync()
                .thenAccept(p -> {
                    p.getTracks().getItems()[0].getAddedAt();
                    log("playlist: " + p.getName());
                });
        //https://open.spotify.com/playlist/5cT02lDTkgoxboOaVBsaPs?si=uPmp_j-HQseT_f_4G1Ptbw
    }

    public CompletableFuture<Track> findTrack(String url) {
        //https://open.spotify.com/track/0Sm93pz6kzCglkjDiCkmYM
        if (url.contains("?")) {
            url = url.split("\\?")[0];
        }
        if (!SPOTIFY_TRACK_URL_PATTERN.matcher(url).matches()) return null;

        var id = url.substring(url.length() - 22); //22 = spotify id length

        log("ID: " + id);

        return spotifyApi.getTrack(id).build().executeAsync();
    }

    public CompletableFuture<AudioTrack> fetchLavaTrack(Track track) {

        String searchQuery = getCachedIdentifier(track);

        if (searchQuery == null) {
            searchQuery = "ytsearch:" + track.getName() + " " + Arrays.stream(track.getArtists()).map(ArtistSimplified::getName).collect(Collectors.joining(" ")) + " topic";
        }

        var future = new CompletableFuture<AudioTrack>();

        log("Attempting to lookup song with identifier " + searchQuery);

        Radio.getInstance().getOrchestrator().getAudioManager().loadItem(searchQuery, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack lavaTrack) {
                saveCachedIdentifier(track, lavaTrack.getIdentifier());
                future.complete(lavaTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                log("Debug: playlist for Spotify search: " + playlist.getTracks().size() + " tracks");

                if (!playlist.getTracks().isEmpty()) {
                    var lavaTrack = playlist.getTracks().get(0);

                    saveCachedIdentifier(track, lavaTrack.getIdentifier());
                    future.complete(lavaTrack);
                }
            }

            @Override
            public void noMatches() {
                log("no matches found!");
                future.complete(null);
            }

            @Override
            public void loadFailed(FriendlyException ex) {
                ex.printStackTrace();
                log("friendly exception!");
                future.complete(null);
            }
        });

        return future;
    }
}
