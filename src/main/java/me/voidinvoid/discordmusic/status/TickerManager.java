package me.voidinvoid.discordmusic.status;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.FormattingUtils;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.managers.ChannelManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TickerManager implements RadioService, SongEventListener {

    private ScheduledExecutorService executor;

    private String lastTickerMessageRadio = "", lastTickerMessageDj = "";

    private ChannelManager djChannel, textChannel;

    private int animator;

    private boolean paused;
    private boolean pausePending;
    private String lyric;
    private AudioTrack activeTrack;

    @Override
    public void onLoad() {
        JDA jda = Radio.getInstance().getJda();
        djChannel = jda.getTextChannelById(RadioConfig.config.channels.djChat).getManager();
        textChannel = jda.getTextChannelById(RadioConfig.config.channels.radioChat).getManager();

        if (this.executor != null) this.executor.shutdown();

        this.executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(() -> {
            animator++;
            update();
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onShutdown() {
        executor.shutdown();
    }

    public void setLyric(String lyric) {
        this.lyric = lyric;
        update();
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        this.activeTrack = track;
        lyric = null;
        update();
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        this.activeTrack = null;
        lyric = null;
        update();
    }

    @Override
    public void onSongPause(boolean paused, Song song, AudioTrack track, AudioPlayer player) {
        this.paused = paused;
        this.activeTrack = track;
        update();
    }

    @Override
    public void onPausePending(boolean isPending) {
        this.pausePending = isPending;
        update();
    }

    private void update() {
        updateTicker(generateTickerMessage(false), generateTickerMessage(true));
    }

    public String generateTickerMessage(boolean dj) {
        StringBuilder sb = new StringBuilder();

        if (dj) {
            if (pausePending) sb.append("🛑");
            if (paused) sb.append("⏸");

            if (pausePending || paused) sb.append(" | ");
        }

        Song s;
        if (activeTrack == null) {

        } else if ((s = activeTrack.getUserData(Song.class)).getType() == SongType.JINGLE) {
            sb.append("🎹 **95 Degrees Radio**");
        } else if (s.getType() == SongType.ADVERTISEMENT) {
            sb.append("📰 **95 Degrees Radio - Advertisement**");
        } else if (s.getType() == SongType.SPECIAL) {
            sb.append("⭐ ");
            sb.append(s.getFriendlyName());
        } else if (s.getType() == SongType.SONG) {
            if (lyric != null) {
                sb.append("📜 ");
                sb.append(FormattingUtils.escapeMarkup(lyric));

                if (dj) {
                    sb.append(" | ");
                }
            }

            if (lyric == null || dj) {
                NetworkSong ns;
                sb.append("🎵 ");
                //round to nearest 20 and check if higher
                if ((((animator + 10) / 20) * 20 > animator) && s instanceof NetworkSong && (ns = (NetworkSong) s).getSuggestedBy() != null) {
                    sb.append("Suggested by: ");
                    sb.append(ns.getSuggestedBy().getName());
                } else {
                    sb.append(s.getFriendlyName());
                }

                if (!activeTrack.getInfo().isStream) {
                    sb.append(" - ");
                    sb.append(FormattingUtils.getFormattedMsTime(activeTrack.getPosition()));
                    sb.append("/");
                    sb.append(FormattingUtils.getFormattedMsTime(activeTrack.getDuration()));
                }
            }
        }

        return sb.toString();
    }

    public void updateTicker(String radio, String dj) {
        if (!lastTickerMessageRadio.equals(dj)) textChannel.setTopic(radio).queue();
        if (!lastTickerMessageDj.equals(dj)) djChannel.setTopic(dj).queue();

        lastTickerMessageRadio = radio;
        lastTickerMessageDj = dj;
    }
}
