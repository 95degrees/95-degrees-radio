package me.voidinvoid.server;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.events.SongEventListener;
import me.voidinvoid.songs.Song;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.List;
import java.util.stream.Collectors;

public class SocketServer implements SongEventListener, EventListener {

    private final SocketIOServer server;
    private SongInfo currentSongInfo;

    private List<MemberInfo> listeners;
    private VoiceChannel voiceChannel;
    private Guild guild;

    public SocketServer(VoiceChannel voiceChannel) {
        this.voiceChannel = voiceChannel;
        guild = voiceChannel.getGuild();

        listeners = voiceChannel.getMembers().stream().filter(m -> !m.getUser().isBot()).map(MemberInfo::new).collect(Collectors.toList());

        Configuration config = new Configuration();
        config.setHostname("0.0.0.0");
        config.setPort(RadioConfig.config.debug ? 9300 : 9500);

        server = new SocketIOServer(config);

        server.addEventListener("request_status", Object.class, (c, o, ack) -> {
            RadioInfo info = new RadioInfo(listeners, guild.getMembers().stream().filter(m -> !m.getUser().isBot()).map(MemberInfo::new).collect(Collectors.toList()), currentSongInfo);

            c.sendEvent("status", info);
        });

        server.startAsync();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    @Override
    public void onSongSeek(AudioTrack track, long seekTime, AudioPlayer player) {
        server.getAllClients().forEach(c -> c.sendEvent("song_seek", seekTime));
    }

    public void updateSongInfo(AudioTrack track, String albumArtUrl) {
        currentSongInfo = new SongInfo(track.getInfo().title, track.getInfo().author, albumArtUrl, System.currentTimeMillis(), System.currentTimeMillis() + track.getDuration());
        server.getAllClients().forEach(c -> c.sendEvent("song_update", currentSongInfo));
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        server.getAllClients().forEach(c -> c.sendEvent("song_update", (Object) null));
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildVoiceJoinEvent || ev instanceof GuildVoiceMoveEvent || ev instanceof GuildVoiceLeaveEvent) {

            if (ev instanceof GuildVoiceJoinEvent) {
                if (!((GuildVoiceJoinEvent) ev).getChannelJoined().equals(voiceChannel)) return;
            } else if (ev instanceof GuildVoiceMoveEvent) {
                GuildVoiceMoveEvent e = (GuildVoiceMoveEvent) ev;
                if (!e.getChannelJoined().equals(voiceChannel) && !e.getChannelLeft().equals(voiceChannel)) return;
            } else {
                if (!((GuildVoiceLeaveEvent) ev).getChannelLeft().equals(voiceChannel)) return;
            }

            if (((GenericGuildVoiceEvent) ev).getMember().getUser().isBot()) return;

            listeners = voiceChannel.getMembers().stream().filter(m -> !m.getUser().isBot()).map(MemberInfo::new).collect(Collectors.toList());

            server.getAllClients().forEach(c -> c.sendEvent("listeners", listeners));
        }
    }
}
