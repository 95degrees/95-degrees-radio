package me.voidinvoid.discordmusic.utils;

import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SpotifySong;
import me.voidinvoid.discordmusic.songs.SpotifyTrackHolder;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import net.dv8tion.jda.api.interactions.button.Button;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class Songs {

    public static String deyoutubeifySong(String song) {

        return song.toLowerCase()
                .replaceAll("\\(official\\)", "")
                .replaceAll("[()\\[\\]{}]", "")
                .replaceAll("official music video", "")
                .replaceAll("official lyrics video", "")
                .replaceAll("official lyric video", "")
                .replaceAll("official video", "")
                .replaceAll("official lyrics", "")
                .replaceAll("lyrics", "")
                .replaceAll("lyric", "")
                .replaceAll("audio", "")
                .replaceAll("music video", "")
                .replaceAll(" video", "")
                .replaceAll("karaoke version", "")
                .replaceAll("full hd", "")
                .replaceAll(" hd", "")
                .replaceAll("4k", "")
                .replaceAll("1080p", "")
                .replaceAll("720p", "");
    }

    public static String titleArtist(Song song) {
        return song.getTitle() + " - " + song.getArtist();
    }

    public static boolean isRatable(Song song) {
        return song instanceof DatabaseSong || song instanceof SpotifySong;
    }

    public static String getLinksMasked(Song song) {
        return getLinks(song).entrySet().stream().map(l -> Formatting.maskLink(l.getValue(), l.getKey().toString())).collect(Collectors.joining(" "));
    }

    public static List<Button> getLinksAsButtons(Song song) {
        return getLinks(song).entrySet().stream().map(l -> Button.link(l.getValue(), l.getKey().getJDAEmoji())).collect(Collectors.toList());
    }

    public static Map<Emoji, String> getLinks(Song song) {
        var links = new LinkedHashMap<Emoji, String>();

        if (song instanceof SpotifyTrackHolder) {
            var st = ((SpotifyTrackHolder) song).getSpotifyTrack();

            if (st != null) {
                links.put(Emoji.SPOTIFY, SpotifyManager.SPOTIFY_TRACK_URL + st.getId());
            }
        }

        if (song instanceof NetworkSong && song.getLavaIdentifier().contains("youtu.be/") || song.getLavaIdentifier().contains("youtube.com/watch?v=")) {
            links.put(Emoji.YOUTUBE, song.getLavaIdentifier());
        }
        //TODO other links

        return links;
    }
}
