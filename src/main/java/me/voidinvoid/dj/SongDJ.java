package me.voidinvoid.dj;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.dj.actions.*;
import me.voidinvoid.events.SongEventListener;
import me.voidinvoid.songs.NetworkSong;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.utils.AlbumArtUtils;
import me.voidinvoid.utils.Colors;
import me.voidinvoid.utils.FormattingUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static me.voidinvoid.utils.FormattingUtils.*;

public class SongDJ implements SongEventListener, EventListener {

    private TextChannel textChannel;

    private List<DJAction> actions = new ArrayList<>();

    private String activeMessageId;

    private AudioTrack activeTrack;

    private SongOrchestrator orchestrator;

    private ScheduledExecutorService executor;

    private ScheduledFuture taskTimer;

    public SongDJ(SongOrchestrator orchestrator, TextChannel textChannel) {

        this.orchestrator = orchestrator;
        this.textChannel = textChannel;

        actions.add(new SkipSongAction());
        actions.add(new PauseSongAction());
        actions.add(new RestartSongAction());
        actions.add(new PlayJingleAction());
        actions.add(new ToggleSuggestionsAction());

        executor = Executors.newScheduledThreadPool(1);

        textChannel.getJDA().addEventListener(this);
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof MessageReactionAddEvent) {
            MessageReactionAddEvent e = (MessageReactionAddEvent) ev;

            if (e.getUser().isBot()) return;
            if (activeMessageId == null) return;

            if (!e.getMessageId().equals(activeMessageId)) return;

            String emote = e.getReaction().getReactionEmote().getName();

            actions.stream().filter(r -> emote.equals(r.getEmoji()) && r.shouldShow(activeTrack)).findFirst().ifPresent(r -> r.invoke(orchestrator, activeTrack, textChannel, e.getUser()));

            e.getReaction().removeReaction(e.getUser()).queue();
        }
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        activeTrack = track;

        List<DJAction> availableActions = this.actions.stream().filter(r -> r.shouldShow(track)).collect(Collectors.toList());

        MessageAction msg = createMessage(song, track, availableActions, player, timeUntilJingle); //send original message and then queue to update every 2 secs

        msg.queue(m -> {
            activeMessageId = m.getId();

            if (!track.getInfo().isStream) {
                taskTimer = executor.scheduleAtFixedRate(() -> editMessage(track, player, timeUntilJingle, m).queue(), 0, 2, TimeUnit.SECONDS);
            }

            availableActions.forEach(a -> m.addReaction(a.getEmoji()).queue());
        });
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        activeTrack = null;
        activeMessageId = null;

        taskTimer.cancel(false);
    }

    public MessageAction createMessage(Song song, AudioTrack track, List<DJAction> actions, AudioPlayer player, int timeUntilJingle) {

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Playing " + getSongType(track))
                .setColor(player.isPaused() ? Colors.ACCENT_PAUSED : Colors.ACCENT_MAIN)
                .addField("Title", track.getInfo().title, true)
                .addField(song instanceof NetworkSong ? "Uploader" : "Artist", FormattingUtils.escapeMarkup(track.getInfo().author), true)
                .addField(song instanceof NetworkSong ? "URL" : "File (#" + (song.getQueue().getSongMap().indexOf(song) + 1) + ")", FormattingUtils.escapeMarkup(song.getLocation()), false)
                .addField("Next Jingle", timeUntilJingle == 0 ? "After this " + FormattingUtils.getSongType(track) + (track.getInfo().isStream ? "" : " (in " + getFormattedMsTimeLabelled(track.getDuration() - track.getPosition()) + ")") : "After " + (timeUntilJingle + 1) + " more songs", false)
                .addField("Elapsed", track.getInfo().isStream ? "-" : getFormattedMsTime(track.getPosition()) + " / " + getFormattedMsTime(track.getDuration()), false)
                .addField("", actions.stream().map(r -> r.getEmoji() + " " + r.getName()).collect(Collectors.joining("\n")), false)
                .setTimestamp(OffsetDateTime.now());

        if (song instanceof NetworkSong) {
            NetworkSong ns = (NetworkSong) song;
            if (ns.getSuggestedBy() != null)
                embed.setFooter(ns.getSuggestedBy().getName(), ns.getSuggestedBy().getAvatarUrl());
        }

        return AlbumArtUtils.attachAlbumArt(embed, song, textChannel);
    }

    public MessageAction editMessage(AudioTrack track, AudioPlayer player, int timeUntilJingle, Message originalMessage) {
        EmbedBuilder embed = new EmbedBuilder(originalMessage.getEmbeds().get(0))
                .setColor(player.isPaused() ? Colors.ACCENT_PAUSED : Colors.ACCENT_MAIN);

        if (timeUntilJingle == 0) {
            embed.getFields().set(3, new MessageEmbed.Field("Next Jingle", "After this " + FormattingUtils.getSongType(track) + " (in " + getFormattedMsTimeLabelled(track.getDuration() - track.getPosition()) + ")", false));
        }

        embed.getFields().set(4, new MessageEmbed.Field("Elapsed", getFormattedMsTime(track.getPosition()) + " / " + getFormattedMsTime(track.getDuration()), false));

        embed.setTimestamp(OffsetDateTime.now());

        return AlbumArtUtils.attachAlbumArtToEdit(embed, track.getUserData(Song.class), originalMessage);
    }

    @Override
    public void onSongLoadError(Song song, FriendlyException error) {
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Failed to Load Track")
                .setColor(Color.RED)
                .setDescription("Failed to load " + song.getLocation() + ".\nCheck the console for stack trace")
                .addField("Error Message", error.getMessage(), false)
                .setTimestamp(OffsetDateTime.now()).build()).queue();
    }

    @Override
    public void onPlaylistChange(SongPlaylist oldPlaylist, SongPlaylist newPlaylist) {
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Playlist")
                .setDescription("Active playlist has been changed")
                .setColor(new Color(230, 230, 230))
                .addField("Name", newPlaylist.getName(), true)
                .setTimestamp(OffsetDateTime.now()).build()).queue();
    }
}