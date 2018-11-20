package me.voidinvoid.discordmusic.karaoke;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.karaoke.lyrics.LyricLine;
import me.voidinvoid.discordmusic.karaoke.lyrics.LyricsFetcher;
import me.voidinvoid.discordmusic.karaoke.lyrics.SongLyrics;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.FormattingUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class KaraokeManager implements SongEventListener {

    private static final long LYRIC_LAG_COMPENSATION_MS = 400L; //always be 0.4s ahead to compensate for lag

    private TextChannel textChannel;
    private TextChannel radioChannel;
    private TextChannel djChannel;

    private ScheduledExecutorService executor;

    private ScheduledFuture taskTimer;

    private Message activeMessage;

    private boolean karaokeMode;

    public KaraokeManager() {

        executor = Executors.newScheduledThreadPool(1);

        radioChannel = Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat);
        djChannel = Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.djChat);
    }

    private boolean initialiseKaraoke() {
        if (textChannel == null)
            textChannel = Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.lyricsChat);
        if (textChannel == null) return false;

        List<Message> lyricsMsgs = textChannel.getHistory().retrievePast(10).complete(); //clear out old messages if for whatever reason there's some there
        User self = textChannel.getJDA().getSelfUser();
        lyricsMsgs.stream().filter(m -> m.getAuthor().equals(self)).forEach(m -> m.delete().queue());

        return true;
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (!karaokeMode) return;

        if (song.getType() != SongType.SONG) return;

        if (track instanceof YoutubeAudioTrack) {
            String[] videoId = track.getIdentifier().split("\\?v=");
            if (videoId.length < 1) return;

            SongLyrics lyrics = LyricsFetcher.findLyricsFor(videoId[0]);

            if (lyrics != null) {
                radioChannel.sendMessage(new EmbedBuilder()
                        .setDescription("Lyrics are available for this song in <#" + textChannel.getId() + ">")
                        .setColor(Colors.ACCENT_KARAOKE_LYRICS)
                        .build()).queue();

                activeMessage = textChannel.sendMessage(new EmbedBuilder()
                        .setTitle("📜 Live song lyrics for **" + FormattingUtils.escapeMarkup(track.getInfo().title) + "**")
                        .setColor(Colors.ACCENT_KARAOKE_LYRICS)
                        .setDescription("...")
                        .build()).complete();

                runLyricTracker(((YoutubeAudioTrack) track), lyrics, activeMessage);
            }
            return;
        }

        textChannel.sendMessage(new EmbedBuilder().setDescription("⚠ Couldn't find song lyrics for " + track.getInfo().title).build()).queue(m -> m.delete().queueAfter(15, TimeUnit.SECONDS));
    }

    private void runLyricTracker(final YoutubeAudioTrack track, final SongLyrics lyrics, Message message) {

        taskTimer = executor.scheduleAtFixedRate(new Runnable() {

            LyricLine lastClosestLyric;

            EmbedBuilder embed = new EmbedBuilder(message.getEmbeds().get(0));

            @Override
            public void run() {
                long elapsed = track.getPosition() + LYRIC_LAG_COMPENSATION_MS;

                LyricLine closestLyric;
                boolean lyricActive;

                if (elapsed / 1000D < lyrics.getLyrics().get(0).getEntryTime()) { //if the song lyrics haven't started yet
                    closestLyric = lyrics.getLyrics().get(0);
                    lyricActive = false;
                } else {
                    closestLyric = lyrics.getActiveLyric(elapsed / 1000D);
                    lyricActive = true;
                }

                if (Objects.equals(closestLyric, lastClosestLyric) && lyricActive)
                    return; //the lyric is at the same pos as last time

                if (closestLyric == null) { //no lyric found, probably between two lyrics
                    if (lastClosestLyric == null) return; //...or not

                    closestLyric = lastClosestLyric;
                    lyricActive = false;
                }

                lastClosestLyric = closestLyric;

                List<LyricLine> lyricsList = lyrics.getLyrics();

                int index = lyricsList.indexOf(closestLyric);
                StringBuilder desc = new StringBuilder();

                for (int i = -1; i <= 6; i++) { //show previous line, current line, and 6 future lines
                    desc.append(i == 0 ? lyricActive ? "➡" : "⬜" : "◾");

                    if (index + i >= 0 && index + i < lyricsList.size()) {
                        String text = lyricsList.get(index + i).getText();
                        if (!text.trim().isEmpty()) {
                            desc.append(i == 0 ? " **" : " ``");
                            desc.append(text);
                            desc.append(i == 0 ? "**" : "``");
                        }
                    }

                    desc.append("\n");
                }

                embed.setDescription(desc);

                message.editMessage(embed.build()).complete();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        if (taskTimer != null) {
            taskTimer.cancel(false);
            activeMessage.delete().queue();
        }
    }

    public boolean isKaraokeMode() {
        return karaokeMode;
    }

    public boolean setKaraokeMode(boolean karaokeMode, TextChannel channel) {
        if (karaokeMode == this.karaokeMode) return karaokeMode;

        this.karaokeMode = karaokeMode;
        this.textChannel = channel;

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Karaoke")
                .setDescription("🎤 Karaoke mode has " + (karaokeMode ? "been activated!" : "ended!"))
                .setTimestamp(OffsetDateTime.now())
                .setColor(new Color(82, 255, 238)).build();

        if (radioChannel != null) radioChannel.sendMessage(embed).queue();
        if (djChannel != null) djChannel.sendMessage(embed).queue();

        if (!karaokeMode) return false;

        if (channel == null && RadioConfig.config.channels.lyricsChat == null) return false;

        return initialiseKaraoke();
    }
}
