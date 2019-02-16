package me.voidinvoid.discordmusic.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.ratings.Rating;
import me.voidinvoid.discordmusic.ratings.SongRatingManager;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.albumart.LocalAlbumArt;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.utils.AlbumArtUtils;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.FormattingUtils;
import me.voidinvoid.discordmusic.utils.reactions.MessageReactionCallbackManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RadioMessageListener implements RadioService, SongEventListener {

    private TextChannel textChannel;

    private Map<String, Long> lastRatingAttempt = new HashMap<>();

    @Override
    public void onLoad() {
        textChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (!song.getType().useAnnouncement()) return;

        EmbedBuilder embed = new EmbedBuilder().setTitle("Now Playing")
                .setColor(new Color(230, 230, 230))
                .addField("Title", song instanceof DatabaseSong ? ((DatabaseSong) song).getTitle() : track.getInfo().title, true)
                .addField(song instanceof NetworkSong ? "Uploader" : "Artist", song instanceof DatabaseSong ? ((DatabaseSong) song).getArtist() : track.getInfo().author, true)
                .setTimestamp(new Date().toInstant());

        if (song instanceof NetworkSong) {
            NetworkSong ns = (NetworkSong) song;
            if (ns.getSuggestedBy() != null)
                embed.setFooter(ns.getSuggestedBy().getName(), ns.getSuggestedBy().getAvatarUrl());
        }

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue(m -> {
            RPCSocketManager srv = Radio.getInstance().getService(RPCSocketManager.class);

            if (srv != null && song.getAlbumArt() instanceof LocalAlbumArt) {
                srv.updateSongInfo(track, m.getEmbeds().get(0).getThumbnail().getUrl(), song instanceof NetworkSong ? ((NetworkSong) song).getSuggestedBy() : null);
            }

            if (song instanceof DatabaseSong) {
                for (Rating r : Rating.values()) {
                    m.addReaction(r.getEmote()).queue();
                }

                MessageReactionCallbackManager cb = Radio.getInstance().getService(MessageReactionCallbackManager.class);

                cb.registerCallback(m.getId(), e -> {
                    String re = e.getReaction().getReactionEmote().getName();
                    for (Rating r : Rating.values()) {
                        if (r.getEmote().equals(re)) {
                            if ((System.currentTimeMillis() - lastRatingAttempt.getOrDefault(e.getUser().getId(), 0L)) < 10000L) {
                                return;
                            }

                            lastRatingAttempt.put(e.getUser().getId(), System.currentTimeMillis());

                            SongRatingManager rm = Radio.getInstance().getService(SongRatingManager.class);
                            rm.rateSong(e.getUser(), (DatabaseSong) song, r);
                            m.getChannel().sendMessage(new EmbedBuilder().setTitle("Song Rating").setColor(Colors.ACCENT_SONG_RATING).setDescription(e.getMember().getAsMention() + ", your rating of **" + FormattingUtils.escapeMarkup(((DatabaseSong) song).getTitle()) + "** has been saved").setTimestamp(OffsetDateTime.now()).setFooter(e.getMember().getUser().getName(), e.getMember().getUser().getAvatarUrl()).build()).queue(m2 -> m2.delete().queueAfter(10, TimeUnit.SECONDS));
                            return;
                        }
                    }
                });
            }
        });
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

        EmbedBuilder embed = new EmbedBuilder().setTitle("Song Queue")
                .setDescription("Added song to the queue")
                .setColor(new Color(230, 230, 230))
                .addField("Title", track.getInfo().title, true)
                .addField("URL", track.getInfo().uri, true)
                .addField("Queue Position", "#" + (queuePosition + 1), false)
                .setTimestamp(OffsetDateTime.now())
                .setFooter(user.getName(), user.getAvatarUrl());

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue();
    }
}
