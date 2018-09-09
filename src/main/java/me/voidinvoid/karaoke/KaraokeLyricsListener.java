package me.voidinvoid.karaoke;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.events.SongEventListener;
import me.voidinvoid.karaoke.lyrics.LyricLine;
import me.voidinvoid.karaoke.lyrics.LyricsFetcher;
import me.voidinvoid.karaoke.lyrics.SongLyrics;
import me.voidinvoid.songs.Song;
import me.voidinvoid.utils.Colors;
import me.voidinvoid.utils.FormattingUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class KaraokeLyricsListener implements SongEventListener {

    private static final long LYRIC_LAG_COMPENSATION_MS = 400L; //always be 0.4s ahead to compensate for lag

    private TextChannel textChannel;

    private ScheduledExecutorService executor;

    private ScheduledFuture taskTimer;

    public KaraokeLyricsListener(TextChannel textChannel) {

        this.textChannel = textChannel;

        List<Message> lyricsMsgs = textChannel.getHistory().retrievePast(10).complete(); //clear out old messages if for whatever reason there's some there
        User self = textChannel.getJDA().getSelfUser();
        lyricsMsgs.stream().filter(m -> m.getAuthor().equals(self)).forEach(m -> m.delete().queue());

        executor = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {

        if (track instanceof YoutubeAudioTrack) {
            String[] videoId = track.getIdentifier().split("\\?v=");
            if (videoId.length < 1) return;

            SongLyrics lyrics = LyricsFetcher.findLyricsFor(videoId[0]);

            if (lyrics == null) return;

            Message msg = textChannel.sendMessage(new EmbedBuilder()
                    .setTitle("📜 Live song lyrics for **" + FormattingUtils.escapeMarkup(song.getTrack().getInfo().title) + "**")
                    .setColor(Colors.ACCENT_KARAOKE_LYRICS)
                    .setDescription("...")
                    .build()).complete();

            runLyricTracker(((YoutubeAudioTrack) track), lyrics, msg);
        }
    }

    public void runLyricTracker(final YoutubeAudioTrack track, final SongLyrics lyrics, Message message) {

        taskTimer = executor.scheduleAtFixedRate(new Runnable() {

            LyricLine lastClosestLyric;
            boolean lastLyricActive;

            EmbedBuilder embed = new EmbedBuilder(message.getEmbeds().get(0));

            RequestFuture editRequest;

            @Override
            public void run() {

                long elapsed = track.getPosition() + LYRIC_LAG_COMPENSATION_MS;

                LyricLine closestLyric;
                boolean lyricActive;

                if (elapsed < lyrics.getLyrics().get(0).getEntryTime()) { //if the song lyrics haven't started yet
                    closestLyric = lyrics.getLyrics().get(0);
                    lyricActive = false;
                } else {
                    closestLyric = lyrics.getActiveLyric(elapsed / 1000D);
                    lyricActive = true;
                }

                if (Objects.equals(closestLyric, lastClosestLyric) && lyricActive == lastLyricActive)
                    return; //the lyric is at the same pos as last time

                if (closestLyric == null) { //no lyric found, probably between two lyrics
                    if (lastClosestLyric == null) return; //...or not

                    closestLyric = lastClosestLyric;
                    lyricActive = false;
                }

                List<LyricLine> lyricsList = lyrics.getLyrics();

                int index = lyricsList.indexOf(closestLyric);
                StringBuilder desc = new StringBuilder();

                for (int i = -1; i <= 6; i++) { //show previous line, current line, and 6 future lines
                    desc.append(i == 0 ? lyricActive ? "⬜" : "➡" : "◼");

                    if (i == 0) desc.append("**");

                    if (index + i >= 0 && index + i < lyricsList.size()) {
                        String text = lyricsList.get(index + i).getText();
                        if (!text.trim().isEmpty()) {
                            desc.append(text);
                        }
                    }

                    if (i == 0) desc.append("**");

                    desc.append("\n");
                }

                embed.setDescription(desc);

                if (editRequest != null) editRequest.cancel(false);
                editRequest = message.editMessage(embed.build()).submit();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        if (taskTimer != null) taskTimer.cancel(false);
    }
}
