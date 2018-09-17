package me.voidinvoid.server;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.events.SongEventListener;
import me.voidinvoid.songs.Song;

public class SocketServer implements SongEventListener {

    private final SocketIOServer server;

    public SocketServer() {

        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(9500);

        server = new SocketIOServer(config);

        server.startAsync();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        SongInfo songInfo = new SongInfo(track.getInfo().title, song.getAlbumArtURL());
        server.getAllClients().forEach(c -> c.sendEvent("song_update", songInfo));
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        server.getAllClients().forEach(c -> c.sendEvent("song_update", null));
    }
}
