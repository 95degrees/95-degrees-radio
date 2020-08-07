package me.voidinvoid.discordmusic.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.lyrics.LiveLyricsManager;
import me.voidinvoid.discordmusic.ratings.Rating;
import me.voidinvoid.discordmusic.ratings.SongRatingManager;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SpotifyTrackHolder;
import me.voidinvoid.discordmusic.songs.UserSuggestable;
import me.voidinvoid.discordmusic.songs.albumart.LocalAlbumArt;
import me.voidinvoid.discordmusic.utils.*;
import me.voidinvoid.discordmusic.utils.cache.CachedChannel;
import me.voidinvoid.discordmusic.utils.reactions.ReactionListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RadioMessageListener implements RadioService, RadioEventListener {

    private TextChannel textChannel;
    private ScheduledExecutorService updaterExecutor;

    private String previousLyric;
    private boolean lyricChangeFlag;

    private long nextForcedUpdate;

    private List<CachedChannel<TextChannel>> statusChannels;

    private List<PersistentMessage> statusMessages;
    private String lastStatusDescription;

    private PersistentMessageManager persistentMessageManager;

    @Override
    public void onLoad() {
        textChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

        persistentMessageManager = Service.of(PersistentMessageManager.class);

        statusMessages = new ArrayList<>();
        statusChannels = new ArrayList<>();

        statusChannels.add(new CachedChannel<>(textChannel));

        if (updaterExecutor != null) {
            updaterExecutor.shutdown();
        }

        updaterExecutor = Executors.newScheduledThreadPool(1);

        updaterExecutor.scheduleAtFixedRate(() -> {
            var prog = generateProgressMessage();

            if (prog == null) return;

            if (nextForcedUpdate > System.currentTimeMillis() && !lyricChangeFlag) return; //don't need to update yet

            if (Objects.equals(lastStatusDescription, prog.getDescription()))
                return; //we're identical to previous message - don't update

            for (var message : statusMessages) {
                persistentMessageManager.editMessage(message, prog);
            }

            lyricChangeFlag = false;
            nextForcedUpdate = System.currentTimeMillis() + 2000;

        }, 2000, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (!song.getType().useAnnouncement()) return;

        var links = Songs.getLinksMasked(song);

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(new Color(230, 230, 230))
                .setTitle("Now Playing")
                .addField(song.getTitle(), song.getArtist() + (links.isBlank() ? "" : "\n\n" + Emoji.LINK + Emoji.DIVIDER_SMALL + links), false);

        if (song instanceof UserSuggestable) {
            var s = (UserSuggestable) song;
            if (s.getSuggestedBy() != null) {
                embed.setFooter("Song suggestion by " + s.getSuggestedBy().getName(), s.getSuggestedBy().getAvatarUrl());
            }
        }

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue(m -> {
            RPCSocketManager srv = Radio.getInstance().getService(RPCSocketManager.class);

            if (srv != null && song.getAlbumArt() instanceof LocalAlbumArt) {
                srv.updateSongInfo(track, m.getEmbeds().get(0).getThumbnail().getUrl(), song instanceof UserSuggestable ? ((UserSuggestable) song).getSuggestedBy() : null);
            }

            var rl = new ReactionListener(m, true);

            if (Songs.isRatable(song)) {

                for (Rating r : Rating.values()) {
                    rl.add(r.getEmote(), ev -> {
                        ev.setCancelled(true);

                        var rm = Radio.getInstance().getService(SongRatingManager.class);
                        rm.rateSong(ev.getMember().getUser(), song, r, false);

                        m.getChannel().sendMessage(new EmbedBuilder()
                                .setTitle("Song Rating")
                                .setColor(Colors.ACCENT_SONG_RATING)
                                .setDescription(ev.getMember().getUser().getAsMention() + ", your rating of **" + Formatting.escape(song.getTitle()) + "** has been saved")
                                .setTimestamp(OffsetDateTime.now()).setFooter(ev.getMember().getUser().getName(), ev.getMember().getUser().getAvatarUrl()).build())
                                .queue(m2 -> m2.delete().queueAfter(10, TimeUnit.SECONDS));
                    });
                }
            }

            rl.add(Emoji.GNOME.getEmote(), ev -> {
                ev.setCancelled(true);

                var sm = Service.of(SkipManager.class);
                sm.addSkipRequest(ev.getMember().getUser());

            });
        });

        var prog = generateProgressMessage();
        if (prog != null) {
            for (var channel : statusChannels) {
                persistentMessageManager.persist(channel.get().sendMessage(prog)).thenAccept(m -> statusMessages.add(m));
            }
            lastStatusDescription = prog.getDescription();
        }
    }

    public void displayMessageUpdates(TextChannel channel) {
        statusChannels.add(new CachedChannel<>(channel));

        var prog = generateProgressMessage();
        if (prog != null) {
            persistentMessageManager.persist(channel.sendMessage(prog)).thenAccept(m -> statusMessages.add(m));
            lastStatusDescription = prog.getDescription();
        }
    }

    private MessageEmbed generateProgressMessage() {
        var track = Radio.getInstance().getOrchestrator().getPlayer().getPlayingTrack();

        try {
            if (track != null && track.isSeekable()) {
                double progPercent = (double) track.getPosition() / (double) track.getDuration();

                var seekBarPosition = (int) (progPercent * 14);

                //var seekBar = Radio.getInstance().getOrchestrator().getPlayer().isPaused() ? Emoji.PAUSE.toString() : Emoji.PLAY.toString();

                String seekBar = Emoji.SEEK_BAR_LEFT_BORDER.toString();

                if (seekBarPosition > 0) {
                    seekBar += Emoji.SEEK_BAR_MID_100.toString().repeat(seekBarPosition);
                }

                var blockProgress = (progPercent * 14d) % 1;

                Emoji blockEmoji;

                if (blockProgress > 0.75) {
                    blockEmoji = Emoji.SEEK_BAR_MID_75;
                } else if (blockProgress > 0.5) {
                    blockEmoji = Emoji.SEEK_BAR_MID_50;
                } else if (blockProgress > 0.25) {
                    blockEmoji = Emoji.SEEK_BAR_MID_25;
                } else {
                    blockEmoji = Emoji.SEEK_BAR_MID_INCOMPLETE;
                }

                seekBar += blockEmoji;

                if (seekBarPosition < 14) {
                    seekBar += Emoji.SEEK_BAR_MID_INCOMPLETE.toString().repeat(13 - seekBarPosition);
                }

                seekBar += Emoji.SEEK_BAR_RIGHT_BORDER;

                var eb = new EmbedBuilder().setDescription(seekBar + " `" + Formatting.getFormattedMsTime(track.getPosition()) + " / " + Formatting.getFormattedMsTime(track.getDuration()) + "`");

                var sm = Service.of(SkipManager.class);
                var skipStatus = sm.generateSkipStatus();
                if (skipStatus != null) {
                    eb.addField("Skip Requests", skipStatus, true);
                }

                eb.setColor(Colors.ACCENT_MAIN); //todo diff colour maybe?
                //var colour = (int) (progPercent * 255);
                //eb.setColor(new Color(colour, colour, colour));

                var lyrics = Service.of(LiveLyricsManager.class).getActiveSongLyrics();
                if (lyrics != null) { //add 200ms to account for lag
                    var lyric = lyrics.getCurrentLine(track.getPosition() + 200).getContent();
                    if (lyric.isEmpty()) {
                        lyric = "...";
                    }

                    eb.appendDescription("\n\n> **" + Formatting.escape(lyric) + "**");

                    if (!lyric.equals(previousLyric)) {
                        lyricChangeFlag = true;
                    }

                    previousLyric = lyric;
                }

                return eb.build();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        statusMessages.forEach(m -> persistentMessageManager.deleteMessage(m));
        statusMessages.clear();
    }

    @Override
    public void onSongQueueError(Song song, AudioTrack track, Member member, NetworkSongError error) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Song Queue")
                .setDescription(error.getErrorMessage())
                .addField("Title", track.getInfo().title, true)
                .addField("URL", track.getInfo().uri, true)
                .setColor(Colors.ACCENT_ERROR)
                .setTimestamp(OffsetDateTime.now());

        if (member != null) {
            User user = member.getUser();
            embed.setFooter(user.getName(), user.getAvatarUrl());
        }

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue();
    }

    @Override
    public void onSongQueued(Song song, AudioTrack track, Member member, int queuePosition) {
        if (member == null || !Radio.getInstance().getOrchestrator().areSuggestionsEnabled()) return;
        User user = member.getUser();

        EmbedBuilder embed = new EmbedBuilder().setTitle("Song Queue")
                .setDescription("Added song to the queue")
                .setColor(new Color(230, 230, 230))
                .addField(song.getTitle(), song.getArtist(), true)
                .addField("Queue Position", "#" + (queuePosition + 1), false)
                .setFooter(user.getName(), user.getAvatarUrl());

        if (song instanceof SpotifyTrackHolder && ((SpotifyTrackHolder) song).getSpotifyTrack() != null) {
            var links = Songs.getLinksMasked(song);
            embed.addField("Links", links, true);
        }

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue();
    }
}
