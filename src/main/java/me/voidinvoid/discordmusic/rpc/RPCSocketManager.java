package me.voidinvoid.discordmusic.rpc;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RPCSocketManager implements RadioService, SongEventListener, EventListener { //todo adapt this for db songs

    private SocketIOServer server;
    private SongInfo currentSongInfo;

    private List<MemberInfo> listeners;
    private Map<SocketIOClient, String> pendingPairCodes;
    private VoiceChannel voiceChannel;
    private Guild guild;

    @Override
    public boolean canRun(RadioConfig config) {
        return config.useSocketServer;
    }

    @Override
    public void onLoad() {
        this.voiceChannel = Radio.getInstance().getJda().getVoiceChannelById(RadioConfig.config.channels.voice);
        this.guild = voiceChannel.getGuild();

        pendingPairCodes = new HashMap<>();

        listeners = voiceChannel.getMembers().stream().filter(m -> !m.getUser().isBot()).map(MemberInfo::new).collect(Collectors.toList());

        if (server == null) {
            Configuration config = new Configuration();
            config.setHostname("0.0.0.0");
            config.setPort(RadioConfig.config.debug ? 9300 : 9500);

            SocketConfig sockets = new SocketConfig();
            sockets.setReuseAddress(true);

            config.setSocketConfig(sockets);

            server = new SocketIOServer(config);

            server.addEventListener("request_status", Object.class, (c, o, ack) -> {
                RadioInfo info = new RadioInfo(listeners, guild.getMembers().stream().filter(m -> !m.getUser().isBot()).map(MemberInfo::new).collect(Collectors.toList()), currentSongInfo);

                c.sendEvent("status", info);
            });

            server.addEventListener("pair_rpc", String.class, (c, o, ack) -> {
                String code = UUID.randomUUID().toString().substring(0, 8);

                pendingPairCodes.put(c, code);
                ack.sendAckData(code);
            });

            server.startAsync();
        }

        // TODO CHECK Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    @Override
    public void onShutdown() {
        if (server != null) server.stop();
    }

    public SocketIOServer getServer() {
        return server;
    }

    @Override
    public void onSongSeek(AudioTrack track, long seekTime, AudioPlayer player) {
        server.getAllClients().forEach(c -> c.sendEvent("song_seek", seekTime));
    }

    public void updateSongInfo(AudioTrack track, String albumArtUrl) {
        Song s = track.getUserData(Song.class);
        DatabaseSong ds = null;
        if (s instanceof DatabaseSong) ds = (DatabaseSong) s;
        currentSongInfo = new SongInfo(ds == null ? track.getInfo().title : ds.getTitle(), ds == null ? track.getInfo().author : ds.getArtist(), albumArtUrl, System.currentTimeMillis(), System.currentTimeMillis() + track.getDuration());
        server.getAllClients().forEach(c -> c.sendEvent("song_update", currentSongInfo));
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        server.getAllClients().forEach(c -> c.sendEvent("song_update", (Object) null));
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildVoiceJoinEvent || ev instanceof GuildVoiceMoveEvent || ev instanceof GuildVoiceLeaveEvent) {

            Member leftMember = null;

            if (ev instanceof GuildVoiceJoinEvent) { // -> JOINED
                if (!ChannelScope.RADIO_VOICE.check(((GuildVoiceJoinEvent) ev).getChannelJoined())) return;

            } else if (ev instanceof GuildVoiceMoveEvent) { // <- LEFT or -> JOINED
                GuildVoiceMoveEvent e = (GuildVoiceMoveEvent) ev;

                if (ChannelScope.RADIO_VOICE.check(e.getChannelLeft())) {
                    leftMember = e.getMember();
                } else if (!ChannelScope.RADIO_VOICE.check(e.getChannelJoined())) {
                    return;
                }

            } else { // <- LEFT
                GuildVoiceLeaveEvent e = (GuildVoiceLeaveEvent) ev;

                if (ChannelScope.RADIO_VOICE.check(e.getChannelLeft())) {
                    leftMember = e.getMember();
                } else {
                    return;
                }
            }

            if (((GenericGuildVoiceEvent) ev).getMember().getUser().isBot()) return;

            listeners = voiceChannel.getMembers().stream().filter(m -> !m.getUser().isBot()).map(MemberInfo::new).collect(Collectors.toList());

            server.getAllClients().forEach(c -> c.sendEvent("listeners", listeners));
        }
    }

    public void sendCoinNotification(String id, int amount, long totalTime) {
        server.getAllClients().forEach(c -> c.sendEvent("coins", new CoinsUpdateInfo(id, amount, totalTime)));
    }
}
