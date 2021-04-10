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
import me.voidinvoid.discordmusic.utils.reactions.MessageReactionCallbackManager;
import me.voidinvoid.discordmusic.utils.reactions.ReactionListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RadioMessageListener implements RadioService, RadioEventListener {

    private CachedChannel<TextChannel> textChannel;
    private List<Message> statusMessages = new ArrayList<>();

    @Override
    public void onLoad() {
        textChannel = new CachedChannel<>(RadioConfig.config.channels.radioChat);
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (!song.getType().useAnnouncement()) return;

        var links = Songs.getLinksMasked(song);

        var timestamp = track.isSeekable() ? Formatting.getFormattedMsTimeLabelled(track.getDuration()) : null;

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(new Color(230, 230, 230))
                .setTitle("Now Playing")
                .addField(song.getTitle(), song.getArtist() + (links.isBlank() ? "" : "\n\n" + Emoji.LINK + Emoji.DIVIDER_SMALL + links), false);

        if (timestamp != null) {
            embed.setFooter(timestamp);
        }

        if (song instanceof UserSuggestable) {
            var s = (UserSuggestable) song;
            if (s.getSuggestedBy() != null) {
                embed.setFooter((timestamp != null ? timestamp + " • " : "") + "Song suggestion by " + s.getSuggestedBy().getName(), s.getSuggestedBy().getAvatarUrl());
            }
        }

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel.get()).queue(m -> {
            statusMessages.add(m);

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

            rl.add(Emoji.SKIP.getEmote(), ev -> {
                ev.setCancelled(true);

                var sm = Service.of(SkipManager.class);
                sm.addSkipRequest(ev.getMember().getUser(), null);

            });
        });
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        statusMessages.forEach(m -> {
            m.clearReactions().queue();
            Service.of(MessageReactionCallbackManager.class).removeCallback(m.getId());
        });

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

        if (song instanceof UserSuggestable) {
            var slash = ((UserSuggestable) song).getSlashCommandSource();

            if (slash != null) {
                AlbumArtUtils.attachAlbumArtToCommandHook(embed, song, slash).queue();

                if (slash.getEvent().getChannel().getId().equals(textChannel.getId())) {
                    return; //if we're not in #radio, send it there too
                }
            }
        }

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel.get()).queue();
    }

    @Override
    public void onSongQueued(Song song, AudioTrack track, Member member, int queuePosition) {
        if (member == null || !Radio.getInstance().getOrchestrator().areSuggestionsEnabled()) return;
        User user = member.getUser();

        var links = "";

        if (song instanceof SpotifyTrackHolder && ((SpotifyTrackHolder) song).getSpotifyTrack() != null) {
            links = Songs.getLinksMasked(song);

            if (links != null && !links.isBlank()) {
                links = "\n\n" + Emoji.LINK + Emoji.DIVIDER_SMALL + links;
            }
        }

        EmbedBuilder embed = new EmbedBuilder().setTitle("Song Queue")
                .setDescription("Song has been added to the queue (#" + (queuePosition + 1) + ")")
                .setColor(new Color(230, 230, 230))
                .addField(song.getTitle(), song.getArtist() + links, true)
                .setFooter(user.getName(), user.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now());

        if (song instanceof UserSuggestable) {
            var slash = ((UserSuggestable) song).getSlashCommandSource();

            if (slash != null) {
                AlbumArtUtils.attachAlbumArtToCommandHook(embed, song, slash).queue();

                if (slash.getEvent().getChannel().getId().equals(textChannel.getId())) {
                    return; //if we're not in #radio, send it there too
                }
            }
        }

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel.get()).queue();
    }
}
