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
import me.voidinvoid.discordmusic.songs.albumart.LocalAlbumArt;
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

public class RadioMessageListener_bak implements RadioService, RadioEventListener, EventListener {

    private TextChannel textChannel;
    private ScheduledExecutorService updaterExecutor;

    private Message activeTitleArtMessage;
    private Message activeProgressMessage;
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
            if (activeProgressMessage != null /*&& (currentEdit == null || currentEdit.isDone())*/) {
                var prog = generateProgressMessage();

                if (nextForcedUpdate > System.currentTimeMillis() && !lyricChangeFlag) return;

                if (prog != null && Objects.equals(activeProgressMessage.getEmbeds().get(0).getDescription(), prog.getDescription()))
                    return;

                if (mostRecentMessageId != null && !mostRecentMessageId.equals(activeProgressMessage.getId())) {
                    activeProgressMessage.delete().queue();

                    activeProgressMessage = null;
                    currentEdit = null;

                    if (prog != null) {
                        textChannel.sendMessage(prog).queue(m -> activeProgressMessage = m);

                        lyricChangeFlag = false;
                        nextForcedUpdate = System.currentTimeMillis() + 2000;
                    }

                    return;
                }

                if (prog != null) {
                    currentEdit = activeProgressMessage.editMessage(prog).submit();

                    lyricChangeFlag = false;
                    nextForcedUpdate = System.currentTimeMillis() + 2000;
                }
            }
        }, 2000, 200, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onShutdown() {
        if (activeProgressMessage != null) {
            activeProgressMessage.delete().queue();
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

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(new Color(230, 230, 230))
                .setTitle("Now Playing")
                .addField(song.getTitle(), song.getArtist(), false)
                .addField("Skip Requests (todo)", "0/0", false);
        // .setTimestamp(OffsetDateTime.now());
        // .setTimestamp(new Date().toInstant());

        if (song instanceof NetworkSong) {
            NetworkSong ns = (NetworkSong) song;
            if (ns.getSuggestedBy() != null) {
                embed.setFooter(ns.getSuggestedBy().getName(), ns.getSuggestedBy().getAvatarUrl());
            }

            /*if (ns.getSpotifyTrack() == null) {
                System.out.println("searching spotify!!");
                Service.of(SpotifyManager.class).searchTrack(track.getInfo().title)
                        .whenComplete((t, e) -> {
                            ns.setSpotifyTrack(t); //TODO this should be done on track creation
                            appendSpotifyTrackDetails(embed, ns.getSpotifyTrack());
                            System.out.println("appened deets! track: " + t);
                            embedReady.complete(embed);
                        });
            } else {
                System.out.println("aaa");
                embedReady.complete(embed);
            }*/
        }

        /*if (song instanceof SpotifyTrackHolder) {
            var sh = (SpotifyTrackHolder) song;

            if (sh.getSpotifyTrack() != null) {
                System.out.println("im a track holder :) track: " + ((SpotifyTrackHolder) song).getSpotifyTrack());
                appendSpotifyTrackDetails(embed, ((SpotifyTrackHolder) song).getSpotifyTrack());
            }
        }

        embedReady.complete(embed);*/

        /*
        EmbedBuilder embed = new EmbedBuilder().setTitle("Now Playing")
                .setColor(new Color(230, 230, 230))
                .addField("Title", song instanceof DatabaseSong ? ((DatabaseSong) song).getTitle() : track.getInfo().title, false)
                .addField(song instanceof NetworkSong ? "Uploader" : "Artist", song instanceof DatabaseSong ? ((DatabaseSong) song).getArtist() : track.getInfo().author, false)
                .setTimestamp(new Date().toInstant());*/


        System.out.println("ready!!");
        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue(m -> {
            activeTitleArtMessage = m;

            RPCSocketManager srv = Radio.getInstance().getService(RPCSocketManager.class);

            if (srv != null && song.getAlbumArt() instanceof LocalAlbumArt) {
                srv.updateSongInfo(track, m.getEmbeds().get(0).getThumbnail().getUrl(), song instanceof NetworkSong ? ((NetworkSong) song).getSuggestedBy() : null);
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
            textChannel.sendMessage(prog).queue(m -> {
                activeProgressMessage = m;
            });
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

                var colour = (int) (progPercent * 255);
                eb.setColor(new Color(colour, colour, colour));

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
        if (activeProgressMessage != null) activeProgressMessage.delete().queue();
        activeProgressMessage = null;
        currentEdit = null;
    }

    //@Override
    public void onSongQueueErroro(NetworkSong song, AudioTrack track, Member member, NetworkSongError error) {
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
    public void onSongQueued(Song song, AudioTrack track, Member member, int queuePosition) {/*
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

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue();*/
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
