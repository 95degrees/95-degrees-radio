package me.voidinvoid.discordmusic.dj;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.dj.actions.*;
import me.voidinvoid.discordmusic.events.RadioEventListener;
import me.voidinvoid.discordmusic.interactions.ButtonManager;
import me.voidinvoid.discordmusic.quiz.QuizPlaylist;
import me.voidinvoid.discordmusic.songs.*;
import me.voidinvoid.discordmusic.utils.*;
import me.voidinvoid.discordmusic.utils.cache.CachedChannel;
import me.voidinvoid.discordmusic.utils.reactions.MessageReactionCallbackManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class SongDJ implements RadioService, RadioEventListener {

    private final List<DJAction> actions = new ArrayList<>();
    private final Map<String, Song> queueDeletionMessages = new HashMap<>();
    private CachedChannel<TextChannel> djChannel, radioChannel;
    private AudioTrack activeTrack;

    private Message currentMessage;

    private Message lastSongLoadErrorMessage;
    private long lastSongLoadError;
    private int lastSongErrorRepeatCount;

    public SongDJ() {

        actions.add(new SkipSongAction());
        actions.add(new PauseSongAction());
        actions.add(new RestartSongAction());
        actions.add(new PauseAtEndAction());
        actions.add(new PlayJingleAction());
        actions.add(new ToggleSuggestionsAction());
        actions.add(new PlayAdvertAction());
    }

    public List<DJAction> getActions() {
        return actions;
    }

    @Override
    public void onLoad() {

        djChannel = new CachedChannel<>(RadioConfig.config.channels.djChat);
        radioChannel = new CachedChannel<>(RadioConfig.config.channels.radioChat);
    }

    public void removeSongFromQueue(Message m, Song song) {

        song.getQueue().remove(song);

        if (m != null) {
            m.delete().reason("Network radio song suggestion removed").queue();
        }

        EmbedBuilder embed = new EmbedBuilder().setTitle("Song Queue")
                .setDescription("Song has been removed from the queue")
                .setColor(new Color(230, 230, 230))
                .addField("Name", song.getTrack().getInfo().title, true)
                .setTimestamp(new Date().toInstant());

        if (song instanceof NetworkSong) {
            embed.addField("URL", song.getTrack().getInfo().uri, true);
        }

        var suggestedBy = song instanceof UserSuggestable ? ((UserSuggestable) song).getSuggestedBy() : null;
        if (suggestedBy != null) {
            embed.setFooter(suggestedBy.getName(), suggestedBy.getAvatarUrl());
        }

        AlbumArtUtils.attachAlbumArt(embed, song, djChannel.get()).queue(); //TODO split into another event? and remove via orchestrator

        if (Radio.getInstance().getOrchestrator().areSuggestionsEnabled()) {
            AlbumArtUtils.attachAlbumArt(embed, song, radioChannel.get()).queue();
        }
    }

    public String invokeAction(DJAction action, User user, ButtonClickEvent event) {
        var res = action.invoke(Radio.getInstance().getOrchestrator(), activeTrack, djChannel.get(), user, event);

        if (event != null && !event.isAcknowledged()) {
            event.deferEdit().queue();
        }

        return res;
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        activeTrack = track;

        if (currentMessage != null) {
            currentMessage.delete().queue();
            currentMessage = null;
        }

        if (Radio.getInstance().getOrchestrator().getActivePlaylist() instanceof QuizPlaylist)
            return; //no quiz stuff here

        if (song instanceof UserSuggestable) {
            for (String id : queueDeletionMessages.keySet()) {
                if (queueDeletionMessages.get(id).equals(song)) {
                    MessageReactionCallbackManager mr = Radio.getInstance().getService(MessageReactionCallbackManager.class);
                    mr.removeCallback(id);
                    djChannel.get().retrieveMessageById(id).queue(m -> m.clearReactions().queue());
                    queueDeletionMessages.remove(id); //remove the ability to cancel this song since it's already playing by now
                    break;
                }
            }
        }

        attachButtons(createMessage(song, track, player, timeUntilJingle)).queue(m -> currentMessage = m);
    }

    public MessageAction attachButtons(MessageAction action) {
        List<DJAction> availableActions = this.actions.stream().filter(r -> r.shouldShow(activeTrack)).collect(Collectors.toList());

        var rowsToCreate = availableActions.stream().map(DJAction::getActionRowIndex).max(Integer::compareTo).orElse(0) + 1;
        var rows = new ActionRow[rowsToCreate];

        for (int i = 0; i < rowsToCreate; i++) {
            int index = i;
            rows[i] = ActionRow.of(availableActions.stream().filter(a -> a.getActionRowIndex() == index).map(a -> ButtonManager.of(ButtonStyle.SECONDARY, net.dv8tion.jda.api.entities.Emoji.fromUnicode(a.getEmoji()), e -> invokeAction(a, e.getEvent().getUser(), e.getEvent()))).collect(Collectors.toList()));
        }

        return action.setActionRows(rows);
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        if (currentMessage != null) currentMessage.delete().queue();

        currentMessage = null;
        activeTrack = null;
    }

    @Override
    public void onSongPause(boolean paused, Song song, AudioTrack track, AudioPlayer player, ButtonClickEvent source) {
        if (currentMessage != null) {
            if (source == null) {
                attachButtons(AlbumArtUtils.attachAlbumArtToEdit(new EmbedBuilder(currentMessage.getEmbeds().get(0)).setColor(paused ? Colors.ACCENT_PAUSED : Colors.ACCENT_MAIN), song, currentMessage)).queue(m -> currentMessage = m);
            } else {
                source.deferEdit().setEmbeds(new EmbedBuilder(currentMessage.getEmbeds().get(0)).setColor(paused ? Colors.ACCENT_PAUSED : Colors.ACCENT_MAIN).build()).queue();
                //AlbumArtUtils.attachAlbumArtToInteractionHook(new EmbedBuilder(currentMessage.getEmbeds().get(0)).setColor(paused ? Colors.ACCENT_PAUSED : Colors.ACCENT_MAIN), song, source.getHook()).queue();
            }
        }
    }

    private MessageAction createMessage(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Playing " + Formatting.getSongType(track))
                .setColor(player.isPaused() ? Colors.ACCENT_PAUSED : Colors.ACCENT_MAIN)
                .setTimestamp(OffsetDateTime.now());

        embed.addField("Title", Formatting.escape(song.getTitle()), true);
        embed.addField("Artist", Formatting.escape(song.getArtist()), true);

        if (song instanceof UserSuggestable && ((UserSuggestable) song).isSuggestion()) { //TODO this all needs sorting with suggested songs......

            var s = (UserSuggestable) song;
            if (s.getSuggestedBy() != null) {
                embed.setFooter("Song suggested by " + s.getSuggestedBy().getName(), s.getSuggestedBy().getAvatarUrl());
            }

        } else if (song.getType() == SongType.SONG) {
            embed.setFooter("(#" + (song.getQueue().getSongMap().indexOf(song) + 1) + " in playlist) " + Formatting.escape(song.getInternalName()));
        }

        embed.addField("Next Jingle", timeUntilJingle == 0 ? "After this " + Formatting.getSongType(track) : "After " + (timeUntilJingle + 1) + " more songs", false);

        var links = Songs.getLinksMasked(song);
        embed.addField("", (links.isBlank() ? "" : Emoji.LINK.toString() + Emoji.DIVIDER_SMALL + links + Emoji.DIVIDER_SMALL) + Formatting.maskLink("https://cdn.discordapp.com/attachments/505174503752728597/537699389255450624/unknown.png", "Help"), false);

        return AlbumArtUtils.attachAlbumArt(embed, song, djChannel.get());
    }

    @Override
    public void onSongLoadError(Song song, FriendlyException error) {
        var now = System.currentTimeMillis();

        if (now - lastSongLoadError >= 10000) { //1 error every 10s...
            lastSongLoadErrorMessage = djChannel.get().sendMessage(new EmbedBuilder()
                    .setColor(Colors.ACCENT_ERROR)
                    .setTitle(Emoji.WARN + " Track load error")
                    .setDescription(Songs.titleArtist(song) + ": " + error.getMessage()).build()).complete();

            lastSongErrorRepeatCount = 0;
        } else if (lastSongLoadErrorMessage != null) { //should always be true
            lastSongErrorRepeatCount++;

            var embed = lastSongLoadErrorMessage.getEmbeds().get(0);
            embed = new EmbedBuilder(embed).setTitle(Emoji.WARN + " Track load error (x" + (lastSongErrorRepeatCount + 1) + ")").build();

            lastSongLoadErrorMessage.editMessage(embed).queue(m -> lastSongLoadErrorMessage = m);
        }

        lastSongLoadError = now;
    }

    @Override
    public void onPlaylistChange(Playlist oldPlaylist, Playlist newPlaylist) {
        djChannel.get().sendMessage(new EmbedBuilder()
                .setTitle("Playlist")
                .setColor(Colors.ACCENT_MAIN)
                .setDescription("Active playlist has been changed")
                .addField("New Playlist", newPlaylist.getName(), true)
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

        djChannel.get().sendMessage(embed.build()).queue();
    }

    @Override
    public void onSongQueued(Song song, AudioTrack track, Member member, int queuePosition) {
        EmbedBuilder embed = new EmbedBuilder().setTitle("Song Queue")
                .setDescription("Added song to the queue")
                .setColor(new Color(230, 230, 230))
                .addField("Title", song.getTitle(), true)
                .addField("Artist", song.getArtist(), true)
                .addField("Identifier", track.getIdentifier(), true)
                .addField("Queue Position", "#" + (queuePosition + 1), false)
                .setTimestamp(OffsetDateTime.now());

        if (member != null) {
            User user = member.getUser();
            embed.setFooter(user.getName(), user.getAvatarUrl());
        }

        AlbumArtUtils.attachAlbumArt(embed, song, djChannel.get()).setActionRows(ActionRow.of(ButtonManager.of(ButtonStyle.DANGER, "Remove", e -> removeSongFromQueue(e.getEvent().getMessage(), song)))).queue();
    }
}