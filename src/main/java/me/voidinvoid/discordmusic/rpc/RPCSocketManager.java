package me.voidinvoid.discordmusic.rpc;

import com.corundumstudio.socketio.*;
import com.google.gson.Gson;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.dj.SongDJ;
import me.voidinvoid.discordmusic.events.NetworkSongError;
import me.voidinvoid.discordmusic.events.RadioEventListener;
import me.voidinvoid.discordmusic.levelling.Achievement;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.lyrics.LiveLyrics;
import me.voidinvoid.discordmusic.ratings.Rating;
import me.voidinvoid.discordmusic.ratings.SongRatingManager;
import me.voidinvoid.discordmusic.songs.*;
import me.voidinvoid.discordmusic.songs.albumart.RemoteAlbumArt;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.songs.local.FileSong;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import me.voidinvoid.discordmusic.suggestions.SongSuggestionManager;
import me.voidinvoid.discordmusic.suggestions.SuggestionQueueMode;
import me.voidinvoid.discordmusic.utils.Service;
import me.voidinvoid.discordmusic.utils.Songs;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class RPCSocketManager implements RadioService, RadioEventListener, EventListener {

    private static final int RPC_VERSION = 3;

    public static final String CLIENT_REQUEST_STATUS = "request_status";
    public static final String CLIENT_IDENTIFY = "identify";
    public static final String CLIENT_RATE_SONG = "rate_song";
    public static final String CLIENT_QUEUE_SONG = "queue_song";
    public static final String CLIENT_QUEUE_SPOTIFY = "queue_spotify";
    public static final String CLIENT_QUEUE_BINARY = "queue_binary";
    public static final String CLIENT_QUEUE_UPLOAD = "queue_upload";
    public static final String CLIENT_DISCONNECT_ME = "disconnect_me";

    //Control panel
    public static final String CLIENT_CONTROL_PLAY_PAUSE_SONG = "dj_pause";
    public static final String CLIENT_CONTROL_RESTART_SONG = "dj_restart";
    public static final String CLIENT_CONTROL_SKIP_SONG = "dj_skip";
    public static final String CLIENT_CONTROL_PAUSE_AFTER_SONG = "dj_pause_after_song";
    public static final String CLIENT_CONTROL_PLAY_JINGLE = "dj_jingle";
    public static final String CLIENT_CONTROL_QUEUE_AD = "dj_ad";
    public static final String CLIENT_CONTROL_TOGGLE_SUGGESTIONS = "dj_toggle_suggestions";
    public static final String CLIENT_CONTROL_CANCEL_UPCOMING_EVENT = "dj_cancel_upcoming_event";

    public static final String SERVER_CONTROL_UPCOMING_EVENTS = "dj_upcoming_events";

    public static final String SERVER_ACCOUNT_LINKED = "linked_account";
    public static final String SERVER_SONG_SEEK = "song_seek";
    public static final String SERVER_SONG_PAUSE = "song_pause";
    public static final String SERVER_SONG_UPDATE = "song_update";
    public static final String SERVER_RADIO_STATUS = "status";
    public static final String SERVER_RADIO_LISTENERS = "listeners";
    public static final String SERVER_COINS = "coins";
    public static final String SERVER_VERSION_INFO = "version";
    public static final String SERVER_IDENTITY = "identity";
    public static final String SERVER_ANNOUNCEMENT = "announcement";
    public static final String SERVER_QUEUE_UPDATE = "queue_update";
    public static final String SERVER_MANUAL_SONG_QUEUED = "song_suggestion";
    public static final String SERVER_MANUAL_SONG_QUEUE_FAILED = "song_suggestion_failed";
    public static final String SERVER_RATING_SAVED = "rating_saved";
    public static final String SERVER_ACHIEVEMENT = "achievement";
    public static final String SERVER_LYRICS = "lyrics";

    //Song manager
    public static final String CLIENT_MANAGER_GET_ALL_PLAYLISTS = "manager_get_all_playlists";
    public static final String CLIENT_MANAGER_GET_PLAYLIST = "manager_get_playlist";
    public static final String CLIENT_MANAGER_SAVE_PLAYLIST = "manager_save_playlist";

    //DJ (OLD)
    //public static final String CLIENT_GET_PLAYLISTS = "dj_get_playlists";
    //public static final String CLIENT_GET_PLAYLIST_SONGS = "dj_get_songs";

    //public static final String SERVER_LIST_PLAYLISTS = "dj_playlists";
    //public static final String SERVER_LIST_PLAYLIST_SONGS = "dj_songs";

    private SocketIOServer server;

    private SongInfo currentSongInfo;
    private LiveLyrics currentLyrics;

    private List<SongInfo> queue;
    private List<UserInfo> listeners;

    private List<UpcomingEvent> upcomingEvents;

    private Map<SocketIOClient, Member> identities;

    private VoiceChannel voiceChannel;
    private TextChannel djChannel;
    private Guild guild;

    private ScheduledExecutorService executorService;

    @Override
    public boolean canRun(RadioConfig config) {
        return config.useSocketServer;
    }

    @Override
    public void onLoad() {
        executorService = Executors.newScheduledThreadPool(1);

        voiceChannel = Radio.getInstance().getJda().getVoiceChannelById(RadioConfig.config.channels.voice);
        djChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.djChat);
        guild = voiceChannel.getGuild();

        identities = new HashMap<>();

        listeners = voiceChannel.getMembers().stream().filter(m -> !m.getUser().isBot()).map(UserInfo::new).collect(Collectors.toList());

        upcomingEvents = new ArrayList<>();

        updateQueue();

        if (server == null) {
            Configuration config = new Configuration();
            config.setHostname("0.0.0.0");
            config.setPort(RadioConfig.config.debug ? 9502 : 9501);
            config.setMaxFramePayloadLength(16777216); //~16mb

            SocketConfig sockets = new SocketConfig();
            sockets.setReuseAddress(true);

            config.setSocketConfig(sockets);

            server = new SocketIOServer(config);

            server.addConnectListener(c -> c.sendEvent(SERVER_VERSION_INFO, RPC_VERSION));

            server.addEventListener(CLIENT_IDENTIFY, String.class, (c, id, ack) -> {

                if (id == null) return;

                try {
                    var user = voiceChannel.getGuild().retrieveMemberById(id).onErrorMap(m -> null).complete();

                    if (user != null) {
                        identities.put(c, user);
                        c.sendEvent(SERVER_IDENTITY, new IdentityInfo(user.getUser()));
                        return;
                    }

                } catch (Exception ignored) {
                }

                c.sendEvent(SERVER_IDENTITY);
            });

            server.addEventListener(CLIENT_REQUEST_STATUS, Object.class, (c, o, ack) -> {
                RadioInfo info = new RadioInfo(RadioConfig.config.voiceInviteLink, listeners, currentSongInfo, queue, upcomingEvents, Radio.getInstance().getOrchestrator().getPlayer().isPaused());

                c.sendEvent(SERVER_RADIO_STATUS, info);
                c.sendEvent(SERVER_LYRICS, currentLyrics);
            });

            server.addEventListener(CLIENT_RATE_SONG, String.class, (c, rating, ack) -> {
                var user = identities.get(c);

                if (user == null) return;

                var srm = Radio.getInstance().getService(SongRatingManager.class);
                var song = Radio.getInstance().getOrchestrator().getCurrentSong();

                if (Songs.isRatable(song) && song.getType() == SongType.SONG) {
                    try {
                        if (!srm.rateSong(user.getUser(), (DatabaseSong) song, Rating.valueOf(rating), false)) {
                            c.sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo("Song rating error", "Please wait before rating songs again"));
                            return;
                        }

                        log("RPC rating saved for user " + user);
                        c.sendEvent(SERVER_RATING_SAVED, rating);
                    } catch (Exception ignored) {
                    }
                }
            });

            server.addEventListener(CLIENT_QUEUE_SONG, String.class, (c, url, ack) -> {
                if (url == null) return;

                var id = identities.get(c);
                if (id == null) return;

                Service.of(AchievementManager.class).rewardAchievement(id.getUser(), Achievement.QUEUE_SONG_WITH_RPC);
                var future = Service.of(SongSuggestionManager.class).addSuggestion(url, null, voiceChannel.getGuild().getTextChannelById(RadioConfig.config.channels.radioChat), id, true, true, SuggestionQueueMode.NORMAL);

                future.thenAccept(res -> {
                   if (!res) {
                       c.sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo("Song queue error", "Song could not be queued as a suitable stream could not be found"));
                   }
                });
            });

            server.addEventListener(CLIENT_QUEUE_SPOTIFY, String.class, (c, url, ack) -> {
                if (url == null) return;

                var id = identities.get(c);
                if (id == null) return;

                var spotify = Service.of(SpotifyManager.class);
                if (spotify == null) return;

                log("Spotify queue: " + url);

                spotify.findTrack(url).thenAccept(t -> {
                    log("Spotify track is null!");
                    if (t == null) return;

                    spotify.fetchLavaTrack(t).whenComplete((s, e) -> {
                        if (e != null) {
                            c.sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo("Song queue error", e.getMessage()));
                            return;
                        }

                        if (s != null) {
                            Radio.getInstance().getOrchestrator().queueSuggestion(new NetworkSong(SongType.SONG, s, id.getUser())).whenComplete((song, ex) -> {
                                if (ex == null) {
                                    c.sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo("Song queue", Songs.titleArtist(song)));
                                } else {
                                    c.sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo("Song queue error", ex.getMessage()));
                                }
                            });
                        } else {
                            log("Song is null?!");
                        }
                    });
                });
            });

            server.addEventListener(CLIENT_QUEUE_BINARY, Object[].class, (c, data, ack) -> {
                log("A");
                if (data == null || data.length < 2) return;

                var id = identities.get(c);
                if (id == null) return;

                String fileName = (String) data[0];

                log(data.length);
                log(fileName);

                byte[] bin = Base64.getDecoder().decode(((String) data[1]).getBytes());

                log("B");

                if (fileName == null || bin.length == 0) return;

                log("c");

                if (fileName.length() > 10) {
                    fileName = fileName.substring(fileName.length() - 10);
                    log("d");
                } //todo add bin length limits (10mb??)

                var file = Files.createTempFile("radiotrack", fileName);

                log("FILENAME: " + file);

                try (var fos = new FileOutputStream(file.toFile())) {
                    fos.write(bin);
                }

                var fs = new FileSong(SongType.SONG, file, id.getUser());

                Radio.getInstance().getOrchestrator().queueSuggestion(fs);

            });

            server.addEventListener(CLIENT_QUEUE_UPLOAD, String.class, (c, file, ack) -> {

                log("Got file: " + file);

                if (file == null) return;

                var id = identities.get(c);
                if (id == null) return;

                if (file.startsWith("radiofiles/")) {
                    file = file.substring(11);
                }

                log("a");
                if (file.contains(File.separator) || file.contains("/") || file.contains("\\")) return; //dont escape the dir we want!

                log("b");

                file = "/home/web/radioserver/radiofiles/" + file;

                log("File: " + file);
                var path = Path.of(file);

                log("Exists: " + Files.exists(path));

                Radio.getInstance().getOrchestrator().createTrack(file).thenAccept(t -> {
                    if (t != null) {
                        Radio.getInstance().getOrchestrator().queueSuggestion(new FileSong(SongType.SONG, path, id.getUser()).setTrack(t)).whenComplete((song, ex) -> {
                            log("Song queue: " + song + ", " + (ex == null ? "no ex" : ex.getMessage()));
                            if (ex != null) {
                                ex.printStackTrace();
                            }
                        });
                    } else {
                        c.sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo("Song queue error", "Unknown error queuing song"));
                    }
                });
            });

            server.addEventListener(CLIENT_MANAGER_GET_PLAYLIST, String.class, (c, playlist, ack) -> {
                log("play11");

                var mb = identities.get(c);

                if (mb == null || !djChannel.canTalk(mb)) return;

                var p = Service.of(DatabaseManager.class).getCollection("playlists").find(eq(playlist)).first();

                log("CLIENT GET A PLAYLIST!! " + playlist);

                ack.sendAckData(p == null ? null : p.toJson());
            });

            server.addEventListener(CLIENT_MANAGER_GET_ALL_PLAYLISTS, Object.class, (c, o, ack) -> {
                log("play22");
                var mb = identities.get(c);

                if (mb == null || !djChannel.canTalk(mb)) return;

                log("CLIENT GET ALL PLAYLISTS!!");

                log(new Gson().toJson(Radio.getInstance().getOrchestrator().getPlaylists().stream().filter(p -> p instanceof RadioPlaylist).map(p -> new PlaylistInfo((RadioPlaylist) p)).collect(Collectors.toList())));

                ack.sendAckData((Object) Radio.getInstance().getOrchestrator().getPlaylists().stream().filter(p -> p instanceof RadioPlaylist).map(p -> new PlaylistInfo((RadioPlaylist) p)).collect(Collectors.toList()));
            });

            server.addEventListener(CLIENT_DISCONNECT_ME, Object.class, (c, o, ack) -> {
                var mb = identities.get(c);

                if (mb == null) return;

                mb.getGuild().kickVoiceMember(mb).queue();
            });

            //DJ actions
            Service.of(SongDJ.class).getActions().forEach(a -> {
                server.addEventListener(a.getSocketCode(), Object.class, (c, o, ack) -> {
                    var mb = identities.get(c);

                    if (mb == null || !djChannel.canTalk(mb)) return;

                    var result = Service.of(SongDJ.class).invokeAction(a, mb.getUser());

                    if (result != null) {
                        c.sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo("DJ controls", result));
                    }
                });
            });

            server.addEventListener(CLIENT_CONTROL_CANCEL_UPCOMING_EVENT, String.class, (c, evId, ack) -> {
                var mb = identities.get(c);

                if (mb == null || !djChannel.canTalk(mb) || upcomingEvents == null) return;

                for (var ev : upcomingEvents) {
                    if (ev.id.equals(evId)) {
                        ev.cancel();
                        updateUpcomingEvents();
                        return;
                    }
                }
            });

            server.startAsync();
        }

        executorService.scheduleAtFixedRate(() -> {
            var track = Radio.getInstance().getOrchestrator().getPlayer().getPlayingTrack();
            if (track != null) {
                server.getBroadcastOperations().sendEvent(SERVER_SONG_SEEK, track.getPosition());
            }
        }, 10, 2, TimeUnit.SECONDS);

        //Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    public Map<SocketIOClient, Member> getIdentities() {
        return identities;
    }

    @Override
    public void onShutdown() {
        if (server != null) server.stop();
    }

    @Override
    public void onSongSeek(AudioTrack track, long seekTime, AudioPlayer player) {
        server.getBroadcastOperations().sendEvent(SERVER_SONG_SEEK, seekTime);
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        //if (!(song.getAlbumArt() instanceof RemoteAlbumArt)) return;
        updateSongInfo(track, song.getAlbumArt() instanceof RemoteAlbumArt ? ((RemoteAlbumArt) song.getAlbumArt()).getUrl() : null, song instanceof NetworkSong ? ((NetworkSong) song).getSuggestedBy() : null);
        sendLyricsUpdate(null);
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        server.getBroadcastOperations().sendEvent(SERVER_SONG_UPDATE, (Object) null);
    }

    @Override
    public void onPlaylistChange(Playlist oldPlaylist, Playlist newPlaylist) {
        updateQueue();
    }

    @Override
    public void onSongPause(boolean paused, Song song, AudioTrack track, AudioPlayer player) {
        server.getBroadcastOperations().sendEvent(SERVER_SONG_PAUSE, paused);
    }

    @Override
    public void onPausePending(boolean isPending) {
        updateUpcomingEvents();
    }

    @Override
    public void onSongQueued(Song song, AudioTrack track, Member member, int queuePosition) {
        updateQueue();
        server.getBroadcastOperations().sendEvent(SERVER_QUEUE_UPDATE, queue);
        server.getBroadcastOperations().sendEvent(SERVER_MANUAL_SONG_QUEUED, new SongInfo(song));
    }

    @Override
    public void onSongQueueError(Song song, AudioTrack track, Member member, NetworkSongError error) {

        var id = member.getUser().getId();

        for (var es : identities.entrySet()) {
            if (es.getValue().getUser().getId().equals(id)) {
                es.getKey().sendEvent(SERVER_MANUAL_SONG_QUEUE_FAILED, error.getErrorMessage());
            }
        }
    }

    @Override
    public void onTrackStopped() {
        server.getBroadcastOperations().sendEvent(SERVER_SONG_PAUSE, true);
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildVoiceJoinEvent || ev instanceof GuildVoiceMoveEvent || ev instanceof GuildVoiceLeaveEvent) {
            var e = (GenericGuildVoiceEvent) ev;

            if (!e.getGuild().equals(guild)) return;
            if (((GenericGuildVoiceEvent) ev).getMember().getUser().isBot()) return;

            listeners = voiceChannel.getMembers().stream().filter(m -> !m.getUser().isBot()).map(UserInfo::new).collect(Collectors.toList());

            server.getAllClients().forEach(c -> c.sendEvent(SERVER_RADIO_LISTENERS, listeners));
        }
    }

    public SocketIOServer getServer() {
        return server;
    }

    public void updateSongInfo(AudioTrack track, String albumArtUrl, User suggestedBy) {
        Song s = track.getUserData(Song.class);

        var start = System.currentTimeMillis() - track.getPosition();

        var orch = Radio.getInstance().getOrchestrator();

        boolean paused = orch.getPlayer() != null && orch.getPlayer().isPaused();

        if (s.getType() != SongType.SONG) {
            currentSongInfo = new SongInfo("95 Degrees Radio", "", albumArtUrl, start, track.getDuration(), false, suggestedBy);
        } else if (s instanceof DatabaseSong) {
            var ds = (DatabaseSong) s;

            currentSongInfo = new SongInfo(ds, albumArtUrl, start, track.getDuration(), suggestedBy);

        } else {
            currentSongInfo = new SongInfo(s.getTitle(), s.getArtist(), albumArtUrl, start, track.getDuration(), false, suggestedBy);
        }

        updateQueue();

        server.getBroadcastOperations().sendEvent(SERVER_SONG_UPDATE, currentSongInfo);
        server.getBroadcastOperations().sendEvent(SERVER_QUEUE_UPDATE, queue);
        server.getBroadcastOperations().sendEvent(SERVER_SONG_PAUSE, orch.getPlayer().isPaused());

        updateUpcomingEvents();
    }

    public void updateUpcomingEvents() {
        var orch = Radio.getInstance().getOrchestrator();

        var pausePending = orch.isPausePending();
        var jinglePending = orch.getTimeUntilJingle() == 0;
        var adPending = !orch.getAwaitingSpecialSongs().isEmpty() && orch.getAwaitingSpecialSongs().get(0).getType() == SongType.ADVERTISEMENT;
        var rewardPending = !orch.getAwaitingSpecialSongs().isEmpty() && orch.getAwaitingSpecialSongs().get(0).getType() == SongType.REWARD;

        var evs = new UpcomingEventsInfo()
                .addIf(UpcomingEventsInfo.ADVERT_EVENT, adPending)
                .addIf(UpcomingEventsInfo.JINGLE_EVENT, jinglePending)
                .addIf(UpcomingEventsInfo.REWARD_EVENT, rewardPending)
                .addIf(UpcomingEventsInfo.PAUSE_EVENT, pausePending)
                .upcomingEvents;

        upcomingEvents = evs;

        for (var client : server.getAllClients()) {
            var mb = identities.get(client);

            if (mb == null || !djChannel.canTalk(mb)) continue;

            client.sendEvent(SERVER_CONTROL_UPCOMING_EVENTS, evs);
        }
    }

    public void sendCoinNotification(String id, int amount, long totalTime) {
        server.getBroadcastOperations().sendEvent(SERVER_COINS, new CoinsUpdateInfo(id, amount, totalTime));
    }

    public void sendAnnouncement(String title, String message) {
        server.getBroadcastOperations().sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo(title, message));
    }

    public void updateQueue() {
        var orch = Radio.getInstance().getOrchestrator();

        if (!orch.isQueueCommandEnabled()) {
            queue = new ArrayList<>();
            return;
        }

        var pl = orch.getActivePlaylist();
        if (pl instanceof RadioPlaylist) {
            queue = ((RadioPlaylist) pl).getSongs().getQueue().stream().limit(10).map(SongInfo::new).collect(Collectors.toList());
        }
    }

    public void notifyAchievement(User user, Achievement achievement) {
        var mb = guild.getMember(user);

        if (mb == null) return; //shouldnt be

        for (var es : identities.entrySet()) {
            if (es.getValue().getUser().getId().equals(user.getId())) {
                es.getKey().sendEvent(SERVER_ACHIEVEMENT, new AchievementInfo(achievement));
            }
        }
    }

    public void sendLyricsUpdate(LiveLyrics lyrics) {
        currentLyrics = lyrics;
        server.getBroadcastOperations().sendEvent(SERVER_LYRICS, lyrics);
    }
}
