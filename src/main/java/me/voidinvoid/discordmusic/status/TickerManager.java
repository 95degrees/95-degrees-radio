package me.voidinvoid.discordmusic.status;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.songs.Song;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.managers.ChannelManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TickerManager implements SongEventListener {

    private ScheduledExecutorService executor;

    private String lastTickerMessage;

    private ChannelManager djChannel, textChannel;

    private boolean paused;
    private String lyric;
    private AudioTrack activeTrack;

    public TickerManager() {
        JDA jda = Radio.getInstance().getJda();
        djChannel = jda.getTextChannelById(RadioConfig.config.channels.djChat).getManager();
        textChannel = jda.getTextChannelById(RadioConfig.config.channels.radioChat).getManager();

        this.executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(() -> {

        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {

    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {

    }

    @Override
    public void onSongPause(boolean paused, Song song, AudioTrack track, AudioPlayer player) {

    }

    public String generateTickerMessage() {
        return "todo lol";
    }

    public void updateTicker(String message) {
        if (message.equals(lastTickerMessage)) return;

        lastTickerMessage = message;

        djChannel.setTopic(message).queue();
        textChannel.setTopic(message).queue();
    }
}
