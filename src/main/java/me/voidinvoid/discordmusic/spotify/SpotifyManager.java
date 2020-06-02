package me.voidinvoid.discordmusic.spotify;

import com.google.gson.Gson;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Track;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.utils.Songs;
import net.dv8tion.jda.api.entities.Member;

import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * DiscordMusic - 30/04/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class SpotifyManager implements RadioService {

    //private static final String SPOTIFY_TRACK_URL = "https://open.spotify.com/track/";
    private static final String SPOTIFY_TRACK_URL_REGEX = "^https?://open.spotify.com/track/[a-zA-Z0-9]{22}$";
    public static final Pattern SPOTIFY_TRACK_URL_PATTERN = Pattern.compile(SPOTIFY_TRACK_URL_REGEX);

    private SpotifyApi spotifyApi;

    @Override
    public void onLoad() {

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

        } catch (Exception ex) {
            log("Error loading Spotify API:");
            ex.printStackTrace();
        }
    }

    public CompletableFuture<Track> searchTrack(String title) {
        var future = new CompletableFuture<Track>();

        title = Songs.deyoutubeifySong(title);

        log("search: " + title);
        spotifyApi.searchTracks(title).build().executeAsync()
                .whenComplete((t, e) -> {
                    log("search complete!");

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

    public CompletableFuture<NetworkSong> queueTrack(Track track, Member suggestedBy) {

        var searchQuery = "ytsearch:" + track.getName() + " " + Arrays.stream(track.getArtists()).map(ArtistSimplified::getName).collect(Collectors.joining(" ")) + " topic";

        var future = new CompletableFuture<NetworkSong>();

        log("Attempting to lookup song with identifier " + searchQuery);

        Radio.getInstance().getOrchestrator().getAudioManager().loadItem(searchQuery, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                queue(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                log("Debug: playlist for Spotify search: " + playlist.getTracks().size() + " tracks");

                if (!playlist.getTracks().isEmpty()) {
                    queue(playlist.getTracks().get(0));
                }
            }

            @Override
            public void noMatches() {
                future.complete(null);
            }

            @Override
            public void loadFailed(FriendlyException ex) {
                future.completeExceptionally(ex);
            }

            private void queue(AudioTrack lavaTrack) {
                Radio.getInstance().getOrchestrator().addNetworkTrack(suggestedBy, lavaTrack, false, false, false,
                        s -> s.setSpotifyTrack(track),
                        future::completeExceptionally);
            }
        });

        return future;
    }
}
