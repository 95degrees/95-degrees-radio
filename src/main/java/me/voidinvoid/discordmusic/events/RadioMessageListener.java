package me.voidinvoid.discordmusic.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.interactions.ButtonManager;
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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.button.Button;
import net.dv8tion.jda.api.interactions.button.ButtonStyle;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RadioMessageListener implements RadioService, RadioEventListener {

    private CachedChannel<TextChannel> textChannel;
    private List<Message> statusMessages = new ArrayList<>();

    private Button SKIP_BUTTON;

    @Override
    public void onLoad() {
        textChannel = new CachedChannel<>(RadioConfig.config.channels.radioChat);

        SKIP_BUTTON = ButtonManager.of(ButtonStyle.SECONDARY, Emoji.SKIP.getJDAEmoji(), e -> {

            e.getEvent().deferEdit().queue();

            var sm = Service.of(SkipManager.class);
            sm.addSkipRequest(e.getEvent().getMember().getUser(), null);
        });
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (!song.getType().useAnnouncement()) return;

        var links = Songs.getLinksAsButtons(song);

        var timestamp = track.isSeekable() ? Formatting.getFormattedMsTimeLabelled(track.getDuration()) : null;

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(new Color(230, 230, 230))
                .setTitle("Now Playing")
                .addField(song.getTitle(), song.getArtist(), false);

        if (timestamp != null) {
            embed.setFooter(timestamp);
        }

        if (song instanceof UserSuggestable) {
            var s = (UserSuggestable) song;
            if (s.getSuggestedBy() != null) {
                embed.setFooter((timestamp != null ? timestamp + " • " : "") + "Song suggestion by " + s.getSuggestedBy().getName(), s.getSuggestedBy().getAvatarUrl());
            }
        }

        var buttons = new ArrayList<Button>();

        if (Songs.isRatable(song)) {
            for (Rating r : Rating.values()) {
                buttons.add(ButtonManager.of(ButtonStyle.SECONDARY, net.dv8tion.jda.api.entities.Emoji.ofUnicode(r.getEmote()), e -> {

                    var rm = Radio.getInstance().getService(SongRatingManager.class);
                    rm.rateSong(e.getEvent().getMember().getUser(), song, r, false);

                    e.getEvent().deferReply(true).addEmbeds(new EmbedBuilder()
                            .setTitle("Song Rating")
                            .setColor(Colors.ACCENT_SONG_RATING)
                            .setDescription(e.getEvent().getMember().getUser().getAsMention() + ", your rating of **" + Formatting.escape(song.getTitle()) + "** has been saved")
                            .setTimestamp(OffsetDateTime.now()).setFooter(e.getEvent().getMember().getUser().getName(), e.getEvent().getMember().getUser().getAvatarUrl()).build()).queue();
                }));
            }
        }

        buttons.add(SKIP_BUTTON);
        buttons.addAll(links);

        ButtonManager.applyButtons(AlbumArtUtils.attachAlbumArt(embed, song, textChannel.get()), buttons).queue(m -> {
            statusMessages.add(m);

            RPCSocketManager srv = Radio.getInstance().getService(RPCSocketManager.class);

            if (srv != null && song.getAlbumArt() instanceof LocalAlbumArt) {
                srv.updateSongInfo(track, m.getEmbeds().get(0).getThumbnail().getUrl(), song instanceof UserSuggestable ? ((UserSuggestable) song).getSuggestedBy() : null);
            }
        });
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        statusMessages.forEach(m -> {
            ButtonManager.applyButtons(m.editMessage(m), Songs.getLinksAsButtons(song)).queue();
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
                AlbumArtUtils.attachAlbumArtToInteractionHook(embed, song, slash).queue();

                if (slash.getInteraction().getChannel().getId().equals(textChannel.getId())) {
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
                AlbumArtUtils.attachAlbumArtToInteractionHook(embed, song, slash).queue();

                if (slash.getInteraction().getChannel().getId().equals(textChannel.getId())) {
                    return; //if we're not in #radio, send it there too
                }
            }
        }

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel.get()).queue();
    }
}
