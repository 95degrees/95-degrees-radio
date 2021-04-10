package me.voidinvoid.discordmusic.lyrics;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.RadioEventListener;
import me.voidinvoid.discordmusic.events.SkipManager;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.SpotifyTrackHolder;
import me.voidinvoid.discordmusic.songs.UserSuggestable;
import me.voidinvoid.discordmusic.utils.*;
import me.voidinvoid.discordmusic.utils.cache.CachedChannel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.bson.Document;

import java.awt.*;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

public class LiveLyricsManager implements RadioService, RadioEventListener {

    private DatabaseManager databaseManager;
    private RPCSocketManager socketManager;

    private MongoCollection<Document> lyrics;
    private boolean enabled = true;
    private LiveLyrics activeSongLyrics;

    private List<Message> lyricsChannelMessages = new ArrayList<>();
    private Message lyricsMessage;
    private String lastLyricsContent;
    private CachedChannel<TextChannel> lyricsChannel;

    private LyricLine previousLyric;
    private boolean lyricChangeFlag;

    private long nextForcedUpdate;

    @Override
    public void onLoad() {
        databaseManager = Service.of(DatabaseManager.class);
        socketManager = Service.of(RPCSocketManager.class);

        lyrics = databaseManager.getCollection("lyrics");
        lyricsChannel = new CachedChannel<>(RadioConfig.config.channels.liveLyrics);

        lyricsChannel.get().getIterableHistory().takeAsync(20).thenAccept(messages -> lyricsChannel.get().purgeMessages(messages));

        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            if (lyricsMessage == null) return;

            var prog = generateProgressMessage();

            if (nextForcedUpdate > System.currentTimeMillis() && !lyricChangeFlag) return; //don't need to update yet

            if (prog == null || Objects.equals(lastLyricsContent, prog.getDescription())) return;

            lyricChangeFlag = false;
            nextForcedUpdate = System.currentTimeMillis() + 2000;

            lastLyricsContent = prog.getDescription();
            lyricsMessage.editMessage(prog).queue();

        }, 2000, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onShutdown() {
        deleteLyricsMessages();
    }

    public CachedChannel<TextChannel> getLyricsChannel() {
        return lyricsChannel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LiveLyrics getActiveSongLyrics() {
        return activeSongLyrics;
    }

    private void deleteLyricsMessages() {
        lyricsChannelMessages.forEach(m -> m.delete().queue());
        lyricsChannelMessages.clear();
        lyricsMessage = null;
    }

    private void createLyricsMessages(Song song) {
        if (!song.getType().useAnnouncement()) return;

        var links = Songs.getLinksMasked(song);

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(new Color(230, 230, 230))
                .setTitle("Now Playing")
                .addField(song.getTitle(), song.getArtist() + (links.isBlank() ? "" : "\n\n" + Emoji.LINK + Emoji.DIVIDER_SMALL + links), false);

        if (song instanceof UserSuggestable) {
            var s = (UserSuggestable) song;
            if (s.getSuggestedBy() != null) {
                embed.setFooter("Song suggestion by " + s.getSuggestedBy().getName(), s.getSuggestedBy().getAvatarUrl());
            }
        }

        AlbumArtUtils.attachAlbumArt(embed, song, getLyricsChannel().get()).queue(m -> lyricsChannelMessages.add(m)); //header

        lastLyricsContent = null;

        var channel = lyricsChannel.get();

        var lyricsMsg = generateProgressMessage();
        if (lyricsMsg != null) {
            channel.sendMessage(lyricsMsg).queue(m -> lyricsChannelMessages.add(lyricsMessage = m));
        }

        generateProgressMessage();
    }

    private MessageEmbed generateProgressMessage() {
        var track = Radio.getInstance().getOrchestrator().getPlayer().getPlayingTrack();

        try {
            if (track != null && track.isSeekable()) {
                double progPercent = (double) track.getPosition() / (double) track.getDuration();

                var seekBarPosition = (int) (progPercent * 14);

                //var seekBar = Radio.getInstance().getOrchestrator().getPlayer().isPaused() ? Emoji.PAUSE.toString() : Emoji.PLAY.toString();

                String seekBar = Emoji.SEEK_BAR_LEFT_BORDER.toString();

                if (seekBarPosition > 0) {
                    seekBar += Emoji.SEEK_BAR_MID_100.toString().repeat(seekBarPosition);
                }

                var blockProgress = (progPercent * 14d) % 1;

                Emoji blockEmoji;

                if (blockProgress > 0.75) {
                    blockEmoji = Emoji.SEEK_BAR_MID_75;
                } else if (blockProgress > 0.5) {
                    blockEmoji = Emoji.SEEK_BAR_MID_50;
                } else if (blockProgress > 0.25) {
                    blockEmoji = Emoji.SEEK_BAR_MID_25;
                } else {
                    blockEmoji = Emoji.SEEK_BAR_MID_INCOMPLETE;
                }

                seekBar += blockEmoji;

                if (seekBarPosition < 14) {
                    seekBar += Emoji.SEEK_BAR_MID_INCOMPLETE.toString().repeat(13 - seekBarPosition);
                }

                seekBar += Emoji.SEEK_BAR_RIGHT_BORDER;

                var eb = new EmbedBuilder().setDescription(seekBar + " `" + Formatting.getFormattedMsTime(track.getPosition()) + " / " + Formatting.getFormattedMsTime(track.getDuration()) + "`");

                var sm = Service.of(SkipManager.class);
                var skipStatus = sm.generateSkipStatus();
                if (skipStatus != null) {
                    eb.addField("Skip Requests", skipStatus, true);
                }

                eb.setColor(Colors.ACCENT_MAIN); //todo diff colour maybe?
                //var colour = (int) (progPercent * 255);
                //eb.setColor(new Color(colour, colour, colour));

                if (activeSongLyrics != null) {

                    eb.appendDescription("\n");

                    var activeLine = activeSongLyrics.getCurrentLine(track.getPosition() + 200);
                    var activeLineIndex = activeSongLyrics.lines.indexOf(activeLine);

                    for (int i = -1; i < 13; i++) {
                        var lineNum = activeLineIndex + i;
                        LyricLine line = lineNum < 0 || lineNum >= activeSongLyrics.lines.size() ? null : activeSongLyrics.lines.get(lineNum);

                        var lineText = "\n" + (line == null ? Emoji.DIVIDER_SMALL : lineNum == activeLineIndex ? "➡ **" + Formatting.escape(line.getContent() + " ") + "**" : Emoji.DIVIDER_SMALL + " " + Formatting.escape(line.getContent()));

                        if (eb.getDescriptionBuilder().length() + lineText.length() > 2048) {
                            break;
                        }

                        eb.appendDescription(lineText);
                    }

                    if (!activeLine.equals(previousLyric)) {
                        lyricChangeFlag = true;
                    }

                    previousLyric = activeLine;
                } else {
                    eb.appendDescription("\n\nNo lyrics are available for this song");
                }

                return eb.build();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        deleteLyricsMessages();

        if (song.getType() != SongType.SONG) return;

        var name = song.getInternalName();

        log("Attempting to fetch lyrics for " + name + "...");

        fetchLyrics(song, track).whenComplete((l, e) -> {
            var ct = Radio.getInstance().getOrchestrator().getPlayer().getPlayingTrack();

            if (track.equals(ct)) {
                socketManager.sendLyricsUpdate(l);
                activeSongLyrics = l;

                createLyricsMessages(song);

                log(activeSongLyrics == null ? "No live lyrics for this song" : "Live lyrics available for this song");
            }
        });
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        activeSongLyrics = null;
    }

    public void removeLyrics(String internalName) {
        lyrics.deleteOne(eq(internalName));
    }

    private CompletableFuture<LiveLyrics> fetchLyrics(Song song, AudioTrack track) {

        var future = new CompletableFuture<LiveLyrics>();

        try {
            var existingLyrics = lyrics.find(eq(song.getInternalName())).first();
            if (existingLyrics != null) {
                log("Lyrics already exist for " + song.getInternalName());

                future.complete(new LiveLyrics(existingLyrics.getString("subtitles")));
                return future;
            }

            if (!enabled) {
                future.complete(null);
                return future;
            }

            var title = song.getTitle();
            var artist = song.getArtist();

            if (song instanceof SpotifyTrackHolder) {
                var s = (SpotifyTrackHolder) song;
                if (s.getSpotifyTrack() == null) {

                    title = Songs.deyoutubeifySong(title);

                    artist = artist.toLowerCase()
                            .replaceAll("vevo", "");
                }
            }

            var cm = new CookieManager();
            cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

            var client = HttpClient.newBuilder().cookieHandler(cm).followRedirects(HttpClient.Redirect.NORMAL).build();

            var request = HttpRequest.newBuilder(URI.create("https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get" +
                    "?format=json" +
                    "&namespace=lyrics_synched" +
                    "&q_artist=" + URLEncoder.encode(artist, StandardCharsets.UTF_8) +
                    "&q_duration=" + track.getDuration() / 1000 +
                    "&q_track=" + URLEncoder.encode(title, StandardCharsets.UTF_8) +
                    "&user_language=en" +
                    "&app_id=web-desktop-app-v1.0" +
                    "&usertoken=200531ee9b232f7d528447be1e137f066f836100534621b21c3027"))
                    .header("cache-control", "no-cache")
                    .header("accept", "*/*")
                    .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Musixmatch/3.14.4564-master.20200505002 Chrome/78.0.3904.130 Electron/7.1.5 Safari/537.36")
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((r, e) -> {
                        if (e != null) {
                            log("Exception fetching lyrics for " + song.getInternalName());
                            e.printStackTrace();
                            future.complete(null);
                            return;
                        }

                        if (r.statusCode() != 200) {
                            log("HTTP error (" + r.statusCode() + ") fetching lyrics for " + song.getInternalName() + ":\n" + r.body());
                            future.complete(null);
                            return;
                        }

                        try {
                            var json = new Gson().fromJson(r.body(), JsonObject.class);

                            var parent = json
                                    .getAsJsonObject("message")
                                    .getAsJsonObject("body")
                                    .getAsJsonObject("macro_calls")
                                    .getAsJsonObject("track.subtitles.get")
                                    .getAsJsonObject("message")
                                    .getAsJsonObject("body");

                            var subtitleList = parent.getAsJsonArray("subtitle_list");

                            if (subtitleList.size() == 0) {
                                log("Subtitle list is empty!");
                                future.complete(null);
                                return;
                            }

                            var subtitles = subtitleList.get(0)
                                    .getAsJsonObject()
                                    .getAsJsonObject("subtitle")
                                    .get("subtitle_body").getAsString();

                            if (subtitles == null) {
                                log("Subtitles are null!");
                                future.complete(null);
                                return;
                            }

                            log("Inserting...");
                            lyrics.insertOne(new Document("_id", song.getInternalName()).append("subtitles", subtitles).append("rawJson", r.body()));

                            log("Fetched lyrics successfully for " + song.getInternalName());
                            future.complete(new LiveLyrics(subtitles));
                        } catch (Exception ignored) {
                            //ex.printStackTrace();
                            future.complete(null);
                        }
                    });
        } catch (Exception ex) {
            ex.printStackTrace();
            future.complete(null);
        }

        return future;
    }
}