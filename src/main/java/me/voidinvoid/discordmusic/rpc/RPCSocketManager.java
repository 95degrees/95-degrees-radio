package me.voidinvoid.discordmusic.rpc;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.dj.SongDJ;
import me.voidinvoid.discordmusic.events.NetworkSongError;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.levelling.Achievement;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.ratings.Rating;
import me.voidinvoid.discordmusic.ratings.SongRatingManager;
import me.voidinvoid.discordmusic.songs.*;
import me.voidinvoid.discordmusic.songs.albumart.RemoteAlbumArt;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import me.voidinvoid.discordmusic.suggestions.SongSuggestionManager;
import me.voidinvoid.discordmusic.suggestions.SuggestionQueueMode;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.bson.Document;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class RPCSocketManager implements RadioService, SongEventListener, EventListener {

    private static final int RPC_VERSION = 3;

    public static final String CLIENT_REQUEST_STATUS = "request_status";
    public static final String CLIENT_PAIR_RPC = "pair_rpc";
    public static final String CLIENT_IDENTIFY = "identify";
    public static final String CLIENT_RATE_SONG = "rate_song";
    public static final String CLIENT_QUEUE_SONG = "queue_song";
    public static final String CLIENT_QUEUE_SPOTIFY = "queue_spotify";
    public static final String CLIENT_UNLINK = "unlink";
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

    public static final String SERVER_RPC_LINK_CODE = "rpc_link_code";
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
    private List<SongInfo> queue;
    private List<UserInfo> listeners;

    private List<UpcomingEvent> upcomingEvents;

    private Map<String, SocketIOClient> pendingPairCodes;
    private Map<SocketIOClient, Member> identities;

    private VoiceChannel voiceChannel;
    private TextChannel djChannel;
    private Guild guild;

    private MongoCollection<Document> linkedAccounts;

    @Override
    public boolean canRun(RadioConfig config) {
        return config.useSocketServer;
    }

    @Override
    public void onLoad() {
        voiceChannel = Radio.getInstance().getJda().getVoiceChannelById(RadioConfig.config.channels.voice);
        djChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.djChat);
        guild = voiceChannel.getGuild();

        linkedAccounts = Radio.getInstance().getService(DatabaseManager.class).getCollection("rpcusers");

        pendingPairCodes = new HashMap<>();
        identities = new HashMap<>();

        listeners = voiceChannel.getMembers().stream().filter(m -> !m.getUser().isBot()).map(UserInfo::new).collect(Collectors.toList());

        upcomingEvents = new ArrayList<>();

        updateQueue();

        if (server == null) {
            Configuration config = new Configuration();
            config.setHostname("0.0.0.0");
            config.setPort(RadioConfig.config.debug ? 9300 : 9500);

            SocketConfig sockets = new SocketConfig();
            sockets.setReuseAddress(true);

            config.setSocketConfig(sockets);

            server = new SocketIOServer(config);

            server.addConnectListener(c -> c.sendEvent(SERVER_VERSION_INFO, RPC_VERSION));

            server.addEventListener(CLIENT_IDENTIFY, String.class, (c, id, ack) -> {

                log(id);
                if (id == null) return;

                var idDoc = linkedAccounts.find(eq(id)).first();
                log(idDoc);

                if (idDoc != null) try {
                    var user = voiceChannel.getGuild().getMemberById(idDoc.getString("discordId"));
                    log(user);
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
            });

            server.addEventListener(CLIENT_PAIR_RPC, Object.class, (c, o, ack) -> {
                String code = UUID.randomUUID().toString().substring(0, 8);

                pendingPairCodes.put(code, c);
                c.sendEvent(SERVER_RPC_LINK_CODE, code);
            });

            server.addEventListener(CLIENT_UNLINK, String.class, (c, code, ack) -> {

                linkedAccounts.deleteOne(eq(code));
                identities.remove(c);

                c.sendEvent(SERVER_IDENTITY);
            });

            server.addEventListener(CLIENT_RATE_SONG, String.class, (c, rating, ack) -> {
                var user = identities.get(c);

                if (user == null) return;

                var srm = Radio.getInstance().getService(SongRatingManager.class);
                var song = Radio.getInstance().getOrchestrator().getCurrentSong();

                if (song instanceof DatabaseSong && song.getType() == SongType.SONG) {
                    try {
                        if (!srm.rateSong(user.getUser(), (DatabaseSong) song, Rating.valueOf(rating), false)) {
                            c.sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo("SONG RATING ERROR", "Please wait before rating songs again"));
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
                Service.of(SongSuggestionManager.class).addSuggestion(url, null, voiceChannel.getGuild().getTextChannelById(RadioConfig.config.channels.radioChat), id, true, SuggestionQueueMode.NORMAL);
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

                    spotify.queueTrack(t, id).whenComplete((s, e) -> {
                        if (e != null) {
                            c.sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo("SONG QUEUE ERROR", e.getMessage()));
                            return;
                        }

                        if (s != null) {
                            Service.of(AchievementManager.class).rewardAchievement(id.getUser(), Achievement.QUEUE_SONG_WITH_RPC);
                            c.sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo("SONG QUEUE", s.getFriendlyName()));
                        } else {
                            log("Song is null?!");
                        }
                    });
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
                        c.sendEvent(SERVER_ANNOUNCEMENT, new AnnouncementInfo("DJ CONTROLS", result));
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

            server.addDisconnectListener(c -> {
                for (String s : pendingPairCodes.keySet()) {
                    if (pendingPairCodes.get(s).equals(c)) {
                        pendingPairCodes.remove(s);
                        return;
                    }
                }
                identities.remove(c);
            });

            server.startAsync();
        }

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
        updateSongInfo(track, ((RemoteAlbumArt) song.getAlbumArt()).getUrl(), song instanceof NetworkSong ? ((NetworkSong) song).getSuggestedBy() : null);
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        server.getBroadcastOperations().sendEvent(SERVER_SONG_UPDATE);
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
    public void onNetworkSongQueued(NetworkSong song, AudioTrack track, Member member, int queuePosition) {
        updateQueue();
        server.getBroadcastOperations().sendEvent(SERVER_QUEUE_UPDATE, queue);
        server.getBroadcastOperations().sendEvent(SERVER_MANUAL_SONG_QUEUED, new SongInfo(song));
    }

    @Override
    public void onNetworkSongQueueError(NetworkSong song, AudioTrack track, Member member, NetworkSongError error) {

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

    public boolean linkAccount(Member member, String linkCode) {
        if (!pendingPairCodes.containsKey(linkCode)) return false;

        var code = UUID.randomUUID().toString();
        linkedAccounts.insertOne(new Document("_id", code).append("discordId", member.getUser().getId()).append("timestamp", System.currentTimeMillis()));

        var c = pendingPairCodes.remove(linkCode);

        c.sendEvent(SERVER_ACCOUNT_LINKED, code);
        c.sendEvent(SERVER_IDENTITY, new IdentityInfo(member.getUser()));

        identities.put(c, member);

        var am = Radio.getInstance().getService(AchievementManager.class);

        am.rewardAchievement(member.getUser(), Achievement.USE_RDP);

        return true;
    }

    public void updateSongInfo(AudioTrack track, String albumArtUrl, User suggestedBy) {
        Song s = track.getUserData(Song.class);

        var start = System.currentTimeMillis() - track.getPosition();
        var end = start + track.getDuration();

        var orch = Radio.getInstance().getOrchestrator();

        boolean paused = orch.getPlayer() != null && orch.getPlayer().isPaused();

        if (s.getType() != SongType.SONG) {
            currentSongInfo = new SongInfo("95 Degrees Radio", "", albumArtUrl, start, end, false, suggestedBy);
        } else if (s instanceof DatabaseSong) {
            var ds = (DatabaseSong) s;

            currentSongInfo = new SongInfo(ds, albumArtUrl, start, end, suggestedBy);

        } else {
            currentSongInfo = new SongInfo(track.getInfo().title, track.getInfo().author, albumArtUrl, start, end, false, suggestedBy);
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
}
