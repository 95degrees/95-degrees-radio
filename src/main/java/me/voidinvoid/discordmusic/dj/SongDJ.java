package me.voidinvoid.discordmusic.dj;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.dj.actions.*;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.quiz.QuizPlaylist;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.songs.local.FileSong;
import me.voidinvoid.discordmusic.utils.AlbumArtUtils;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.Formatting;
import me.voidinvoid.discordmusic.utils.reactions.MessageReactionCallbackManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class SongDJ implements RadioService, SongEventListener, EventListener {

    private TextChannel djChannel, radioChannel;

    private List<DJAction> actions = new ArrayList<>();

    private Map<String, NetworkSong> queueDeletionMessages = new HashMap<>();

    private AudioTrack activeTrack;

    private Message currentMessage;

    public SongDJ() {

        actions.add(new SkipSongAction());
        actions.add(new PauseSongAction());
        actions.add(new RestartSongAction());
        actions.add(new PauseAtEndAction());
        actions.add(new PlayJingleAction());
        actions.add(new ToggleSuggestionsAction());
        actions.add(new PlayAdvertAction());
    }

    @Override
    public void onLoad() {

        djChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.djChat);
        radioChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);
    }

    public void removeSongFromQueue(Message m, NetworkSong song) {

        song.getQueue().remove(song);

        m.delete().reason("Network radio song suggestion removed").queue();

        EmbedBuilder embed = new EmbedBuilder().setTitle("Song Queue")
                .setDescription("Song has been removed from the queue")
                .setColor(new Color(230, 230, 230))
                .addField("Name", song.getTrack().getInfo().title, true)
                .addField("URL", song.getTrack().getInfo().uri, true)
                .setTimestamp(new Date().toInstant());

        if (song.getSuggestedBy() != null) {
            embed.setFooter(song.getSuggestedBy().getName(), song.getSuggestedBy().getAvatarUrl());
        }

        AlbumArtUtils.attachAlbumArt(embed, song, djChannel).queue(); //TODO split into another event? and remove via orchestrator

        if (Radio.getInstance().getOrchestrator().areSuggestionsEnabled()) {
            AlbumArtUtils.attachAlbumArt(embed, song, radioChannel).queue();
        }
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) { //todo use MessageReactionCallbackManager
        if (ev instanceof MessageReactionAddEvent) {
            MessageReactionAddEvent e = (MessageReactionAddEvent) ev;

            if (e.getUser().isBot()) return;

            if (currentMessage == null) return;

            if (!e.getMessageId().equals(currentMessage.getId())) return;

            String emote = e.getReaction().getReactionEmote().getName();

            actions.stream().filter(r -> emote.equals(r.getEmoji()) && r.shouldShow(activeTrack)).findAny().ifPresent(r -> r.invoke(Radio.getInstance().getOrchestrator(), activeTrack, djChannel, e.getUser()));

            e.getReaction().removeReaction(e.getUser()).queue();
        }
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        activeTrack = track;

        if (currentMessage != null) currentMessage.delete().queue();

        if (Radio.getInstance().getOrchestrator().getActivePlaylist() instanceof QuizPlaylist)
            return; //no quiz stuff here

        if (song instanceof NetworkSong) {
            for (String id : queueDeletionMessages.keySet()) {
                if (queueDeletionMessages.get(id).equals(song)) {
                    MessageReactionCallbackManager mr = Radio.getInstance().getService(MessageReactionCallbackManager.class);
                    mr.removeCallback(id);
                    djChannel.retrieveMessageById(id).queue(m -> m.clearReactions().queue());
                    queueDeletionMessages.remove(id); //remove the ability to cancel this song since it's already playing by now
                    break;
                }
            }
        }

        List<DJAction> availableActions = this.actions.stream().filter(r -> r.shouldShow(track)).collect(Collectors.toList());

        createMessage(song, track, player, timeUntilJingle).queue(m -> {
            currentMessage = m;
            availableActions.forEach(a -> m.addReaction(a.getEmoji()).queue());
        });
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        if (currentMessage != null) currentMessage.delete().queue();

        currentMessage = null;
        activeTrack = null;
    }

    @Override
    public void onSongPause(boolean paused, Song song, AudioTrack track, AudioPlayer player) {
        if (currentMessage != null) {
            AlbumArtUtils.attachAlbumArtToEdit(new EmbedBuilder(currentMessage.getEmbeds().get(0)).setColor(paused ? Colors.ACCENT_PAUSED : Colors.ACCENT_MAIN), song, currentMessage).queue(m -> currentMessage = m);
        }
    }

    private MessageAction createMessage(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Playing " + Formatting.getSongType(track))
                .setColor(player.isPaused() ? Colors.ACCENT_PAUSED : Colors.ACCENT_MAIN)
                .setTimestamp(OffsetDateTime.now());

        if (song instanceof NetworkSong) {
            embed.addField("Title", Formatting.escape(track.getInfo().title), true);
            embed.addField("Uploader", Formatting.escape(track.getInfo().author), true);
            embed.addField("URL", Formatting.escape(song.getFileName()), true);

            NetworkSong ns = (NetworkSong) song;
            if (ns.getSuggestedBy() != null) {
                embed.setFooter(ns.getSuggestedBy().getName(), ns.getSuggestedBy().getAvatarUrl());
            }

        } else if (song instanceof DatabaseSong) {
            DatabaseSong ds = (DatabaseSong) song;

            embed.addField("Title", Formatting.escape(ds.getTitle()), true);
            embed.addField("Artist", Formatting.escape(ds.getArtist()), true);
            embed.addField("Album Art ID", "(#" + (song.getQueue().getSongMap().indexOf(song) + 1) + ") " + Formatting.escape(ds.getFileName()), true);
            embed.addField("MBID", ds.getMbId() == null ? "Unknown" : Formatting.escape(ds.getMbId()), true);

        } else if (song instanceof FileSong) {

            if (song.getType() == SongType.SONG) {
                embed.addField("Title", Formatting.escape(track.getInfo().title), true);
                embed.addField("Artist", Formatting.escape(track.getInfo().author), true);
                embed.addField("File Path", "(#" + (song.getQueue().getSongMap().indexOf(song) + 1) + ") " + Formatting.escape(song.getFileName()), true);
            }

        } else {
            embed.addField("Unknown Track Details", "😢", false);
        }

        embed.addField("Next Jingle", timeUntilJingle == 0 ? "After this " + Formatting.getSongType(track) : "After " + (timeUntilJingle + 1) + " more songs", false);
        embed.addField("", "[Control Panel Help](https://cdn.discordapp.com/attachments/505174503752728597/537699389255450624/unknown.png)", false);

        return AlbumArtUtils.attachAlbumArt(embed, song, djChannel);
    }

    @Override
    public void onSongLoadError(Song song, FriendlyException error) {
        djChannel.sendMessage(new EmbedBuilder()
                .setTitle("Failed to Load Track")
                .setColor(Colors.ACCENT_ERROR)
                .setDescription("⚠ Failed to load " + song.getFriendlyName() + ".\nCheck the console for stack trace")
                .addField("Error Message", error.getMessage(), false)
                .setTimestamp(OffsetDateTime.now()).build()).queue();
    }

    @Override
    public void onPlaylistChange(Playlist oldPlaylist, Playlist newPlaylist) {
        djChannel.sendMessage(new EmbedBuilder()
                .setTitle("Playlist")
                .setColor(Colors.ACCENT_MAIN)
                .setDescription("Active playlist has been changed")
                .addField("Name", newPlaylist.getName(), true)
                .setTimestamp(OffsetDateTime.now()).build()).queue();
    }

    @Override
    public void onSuggestionsToggle(boolean enabled, User source) {
        if (source == null) return;

        EmbedBuilder embed = new EmbedBuilder().setTitle("Song Suggestions")
                .setColor(Colors.ACCENT_MAIN)
                .setDescription("Song suggestions have been " + (enabled ? "enabled" : "disabled"))
                .setTimestamp(OffsetDateTime.now())
                .setFooter(source.getName(), source.getAvatarUrl());

        djChannel.sendMessage(embed.build()).queue();
    }

    @Override
    public void onNetworkSongQueued(NetworkSong song, AudioTrack track, Member member, int queuePosition) {
        EmbedBuilder embed = new EmbedBuilder().setTitle("Song Queue")
                .setDescription("Added song to the queue")
                .setColor(new Color(230, 230, 230))
                .addField("Title", track.getInfo().title, true)
                .addField("URL", track.getInfo().uri, true)
                .addField("Queue Position", "#" + (queuePosition + 1), false)
                .setTimestamp(OffsetDateTime.now());

        if (member != null) {
            User user = member.getUser();
            embed.setFooter(user.getName(), user.getAvatarUrl());
        }

        MessageReactionCallbackManager mr = Radio.getInstance().getService(MessageReactionCallbackManager.class);

        AlbumArtUtils.attachAlbumArt(embed, song, djChannel).queue(m -> {
            m.addReaction("❌").queue();
            queueDeletionMessages.put(m.getId(), song);
            mr.registerCallback(m.getId(), e -> removeSongFromQueue(m, song));
        });
    }
}