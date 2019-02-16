package me.voidinvoid.discordmusic.rpc;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.mongodb.client.MongoCollection;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.NetworkSongError;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.levelling.Achievement;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.ratings.Rating;
import me.voidinvoid.discordmusic.ratings.SongRatingManager;
import me.voidinvoid.discordmusic.songs.*;
import me.voidinvoid.discordmusic.songs.albumart.RemoteAlbumArt;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.suggestions.SongSuggestionManager;
import me.voidinvoid.discordmusic.suggestions.SuggestionQueueMode;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

public class RPCSocketManager implements RadioService, SongEventListener, EventListener { //todo adapt this for db songs

    private static final int RPC_VERSION = 2;

    private static final String CLIENT_REQUEST_STATUS = "request_status";
    private static final String CLIENT_PAIR_RPC = "pair_rpc";
    private static final String CLIENT_IDENTIFY = "identify";
    private static final String CLIENT_RATE_SONG = "rate_song";
    private static final String CLIENT_QUEUE_SONG = "queue_song";
    private static final String CLIENT_UNLINK = "unlink";

    private static final String SERVER_RPC_LINK_CODE = "rpc_link_code";
    private static final String SERVER_ACCOUNT_LINKED = "linked_account";
    private static final String SERVER_SONG_SEEK = "song_seek";
    private static final String SERVER_SONG_UPDATE = "song_update";
    private static final String SERVER_RADIO_STATUS = "status";
    private static final String SERVER_RADIO_LISTENERS = "listeners";
    private static final String SERVER_COINS = "coins";
    private static final String SERVER_VERSION_INFO = "version";
    private static final String SERVER_IDENTITY = "identity";
    private static final String SERVER_ANNOUNCEMENT = "announcement";
    private static final String SERVER_QUEUE_UPDATE = "queue_update";
    private static final String SERVER_MANUAL_SONG_QUEUED = "song_suggestion";
    private static final String SERVER_MANUAL_SONG_QUEUE_FAILED = "song_suggestion_failed";
    private static final String SERVER_RATING_SAVED = "rating_saved";
    private static final String SERVER_ACHIEVEMENT = "achievement";

    private SocketIOServer server;

    private SongInfo currentSongInfo;
    private List<SongInfo> queue;
    private List<UserInfo> listeners;

    private Map<String, SocketIOClient> pendingPairCodes;
    private Map<SocketIOClient, Member> identities;

    private VoiceChannel voiceChannel;
    private Guild guild;

    private MongoCollection<Document> linkedAccounts;

    @Override
    public boolean canRun(RadioConfig config) {
        return config.useSocketServer;
    }

    @Override
    public void onLoad() {
        voiceChannel = Radio.getInstance().getJda().getVoiceChannelById(RadioConfig.config.channels.voice);
        guild = voiceChannel.getGuild();

        linkedAccounts = Radio.getInstance().getService(DatabaseManager.class).getCollection("rpcusers");

        pendingPairCodes = new HashMap<>();
        identities = new HashMap<>();

        listeners = voiceChannel.getMembers().stream().filter(m -> !m.getUser().isBot()).map(UserInfo::new).collect(Collectors.toList());

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
                RadioInfo info = new RadioInfo(listeners, currentSongInfo, queue);

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
                        srm.rateSong(user.getUser(), (DatabaseSong) song, Rating.valueOf(rating));
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

                Radio.getInstance().getService(SongSuggestionManager.class).addSuggestion(url, null, voiceChannel.getGuild().getTextChannelById(RadioConfig.config.channels.radioChat), id, true, SuggestionQueueMode.NORMAL);
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
        if (!(song.getAlbumArt() instanceof RemoteAlbumArt)) return;
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
    public void onEvent(Event ev) {
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
    }

    public void sendCoinNotification(String id, int amount, long totalTime) {
        server.getBroadcastOperations().sendEvent(SERVER_COINS, new CoinsUpdateInfo(id, amount, totalTime));
    }

    public void sendAnnouncement(String message) {
        server.getBroadcastOperations().sendEvent(SERVER_ANNOUNCEMENT, message);
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
