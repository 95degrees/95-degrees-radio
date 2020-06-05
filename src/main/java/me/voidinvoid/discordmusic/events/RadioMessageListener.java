package me.voidinvoid.discordmusic.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.lyrics.LiveLyricsManager;
import me.voidinvoid.discordmusic.ratings.Rating;
import me.voidinvoid.discordmusic.ratings.SongRatingManager;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SpotifyTrackHolder;
import me.voidinvoid.discordmusic.songs.albumart.LocalAlbumArt;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import me.voidinvoid.discordmusic.utils.*;
import me.voidinvoid.discordmusic.utils.reactions.ReactionListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RadioMessageListener implements RadioService, SongEventListener, EventListener {

    private TextChannel textChannel;
    private ScheduledExecutorService updaterExecutor;

    private Message activeMessage;
    private CompletableFuture currentEdit;

    private String mostRecentMessageId;

    private String previousLyric;
    private boolean lyricChangeFlag;

    private long nextForcedUpdate;

    @Override
    public void onLoad() {
        textChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

        if (updaterExecutor != null) {
            updaterExecutor.shutdown();
        }

        updaterExecutor = Executors.newScheduledThreadPool(1);

        updaterExecutor.scheduleAtFixedRate(() -> {
            if (activeMessage != null /*&& (currentEdit == null || currentEdit.isDone())*/) {
                var prog = generateProgressMessage();

                if (nextForcedUpdate > System.currentTimeMillis() && !lyricChangeFlag) return;

                if (prog != null && Objects.equals(activeMessage.getEmbeds().get(0).getDescription(), prog.getDescription()))
                    return;

                if (mostRecentMessageId != null && !mostRecentMessageId.equals(activeMessage.getId())) {
                    activeMessage.delete().queue();

                    activeMessage = null;
                    currentEdit = null;

                    if (prog != null) {
                        textChannel.sendMessage(prog).queue(m -> activeMessage = m);

                        lyricChangeFlag = false;
                        nextForcedUpdate = System.currentTimeMillis() + 2000;
                    }

                    return;
                }

                if (prog != null) {
                    currentEdit = activeMessage.editMessage(prog).submit();

                    lyricChangeFlag = false;
                    nextForcedUpdate = System.currentTimeMillis() + 2000;
                }
            }
        }, 2000, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onShutdown() {
        if (activeMessage != null) {
            activeMessage.delete().queue();
        }
    }

    private void appendSpotifyTrackDetails(EmbedBuilder builder, Track track) {
        if (track == null) return;

        builder.clearFields();
        builder.addField("[Spotify] **" + track.getName() + "**",
                track.getArtists()[0].getName() + "\n ", false);
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (!song.getType().useAnnouncement()) return;

        var embedReady = new CompletableFuture<EmbedBuilder>();

        EmbedBuilder embed = new EmbedBuilder()
                .setDescription("\u200b\n")
                .setColor(new Color(230, 230, 230))
                .addField("**" + song.getTitle() + "**",
                        song.getArtist() + "\n\u200b", false);
        // .setTimestamp(new Date().toInstant());

        if (song instanceof NetworkSong) {
            NetworkSong ns = (NetworkSong) song;
            if (ns.getSuggestedBy() != null) {
                embed.setFooter(ns.getSuggestedBy().getName(), ns.getSuggestedBy().getAvatarUrl());
            }

            if (ns.getSpotifyTrack() == null) {
                Service.of(SpotifyManager.class).searchTrack(track.getInfo().title)
                        .whenComplete((t, e) -> {
                            appendSpotifyTrackDetails(embed, ns.getSpotifyTrack());
                            embedReady.complete(embed);
                        });
            }
        } else {
            embedReady.complete(embed);
        }

        if (song instanceof SpotifyTrackHolder) {
            appendSpotifyTrackDetails(embed, ((SpotifyTrackHolder) song).getSpotifyTrack());
            embedReady.complete(embed);
        }

        /*
        EmbedBuilder embed = new EmbedBuilder().setTitle("Now Playing")
                .setColor(new Color(230, 230, 230))
                .addField("Title", song instanceof DatabaseSong ? ((DatabaseSong) song).getTitle() : track.getInfo().title, false)
                .addField(song instanceof NetworkSong ? "Uploader" : "Artist", song instanceof DatabaseSong ? ((DatabaseSong) song).getArtist() : track.getInfo().author, false)
                .setTimestamp(new Date().toInstant());*/


        embedReady.thenAccept(em -> {
            AlbumArtUtils.attachAlbumArt(em, song, textChannel).queue(m -> {

                RPCSocketManager srv = Radio.getInstance().getService(RPCSocketManager.class);

                if (srv != null && song.getAlbumArt() instanceof LocalAlbumArt) {
                    srv.updateSongInfo(track, m.getEmbeds().get(0).getThumbnail().getUrl(), song instanceof NetworkSong ? ((NetworkSong) song).getSuggestedBy() : null);
                }

                if (Songs.isRatable(song)) {

                    var rl = new ReactionListener(m);

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

                    rl.add(Emoji.GNOME.getEmote(), ev -> {
                        ev.setCancelled(true);
                    });
                }
            });

            var prog = generateProgressMessage();
            if (prog != null) {
                textChannel.sendMessage(prog).queue(m -> {
                    activeMessage = m;
                });
            }
        });
    }

    private MessageEmbed generateProgressMessage() {
        var track = Radio.getInstance().getOrchestrator().getPlayer().getPlayingTrack();

        try {
            if (track != null && track.isSeekable()) {
                double progPercent = (double) track.getPosition() / (double) track.getDuration();

                var seekBarPosition = (int) (progPercent * 14);

                var seekBar = Radio.getInstance().getOrchestrator().getPlayer().isPaused() ? Emoji.PAUSE.toString() : Emoji.PLAY.toString();

                seekBar += " ";

                if (seekBarPosition > 0) {
                    seekBar += Emoji.SEEK_COMPLETE_START;
                    seekBar += Emoji.SEEK_MID_COMPLETE.toString().repeat(seekBarPosition - 1);
                }

                if ((progPercent * 14d) % 1 < 0.5) {
                    seekBar += seekBarPosition == 0 ? Emoji.SEEK_LEFT_EDGE_ACTIVE : Emoji.SEEK_ACTIVE;
                } else {
                    seekBar += seekBarPosition == 0 ? Emoji.SEEK_LEFT_EDGE_ACTIVE_LEFT : Emoji.SEEK_ACTIVE_LEFT;
                    seekBar += seekBarPosition == 14 ? Emoji.SEEK_RIGHT_EDGE_ACTIVE_RIGHT : Emoji.SEEK_ACTIVE_RIGHT;
                    seekBarPosition++;
                }

                if (seekBarPosition != 14) {
                    seekBar += Emoji.SEEK_MID.toString().repeat(14 - seekBarPosition);
                    seekBar += Emoji.SEEK_END;
                }

                var eb = new EmbedBuilder().setDescription(seekBar + " `" + Formatting.getFormattedMsTime(track.getPosition()) + " / " + Formatting.getFormattedMsTime(track.getDuration()) + "`");

                var colour = (int) (progPercent * 255);
                eb.setColor(new Color(colour, colour, colour));

                var lyrics = Service.of(LiveLyricsManager.class).getActiveSongLyrics();
                if (lyrics != null) { //add 200ms to account for lag
                    var lyric = lyrics.getCurrentLine(track.getPosition() + 200).getContent();
                    if (lyric.isEmpty()) {
                        lyric = "...";
                    }

                    eb.appendDescription("\n\n**" + Formatting.escape(lyric) + "**");

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
        if (activeMessage != null) activeMessage.delete().queue();
        activeMessage = null;
        currentEdit = null;
    }

    @Override
    public void onNetworkSongQueueError(NetworkSong song, AudioTrack track, Member member, NetworkSongError error) {
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
    public void onNetworkSongQueued(NetworkSong song, AudioTrack track, Member member, int queuePosition) {
        if (member == null || !Radio.getInstance().getOrchestrator().areSuggestionsEnabled()) return;
        User user = member.getUser();

        var title = song.getSpotifyTrack() != null ? song.getSpotifyTrack().getName() : track.getInfo().title;
        var artist = song.getSpotifyTrack() != null ? song.getSpotifyTrack().getArtists()[0].getName() : track.getInfo().uri;

        EmbedBuilder embed = new EmbedBuilder().setTitle("Song Queue")
                .setDescription("Added song to the queue")
                .setColor(new Color(230, 230, 230))
                .addField("Title", title, true)
                .addField(song.getSpotifyTrack() != null ? "Artist" : "URL", artist, true)
                .addField("Queue Position", "#" + (queuePosition + 1), false)
                .setTimestamp(OffsetDateTime.now())
                .setFooter(user.getName(), user.getAvatarUrl());

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue();
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildMessageReceivedEvent) {
            if (((GuildMessageReceivedEvent) ev).getChannel().equals(textChannel)) {
                mostRecentMessageId = ((GuildMessageReceivedEvent) ev).getMessageId();
            }
        }
    }
}
