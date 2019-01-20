package me.voidinvoid.discordmusic.dj;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.dj.actions.*;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.karaoke.KaraokeManager;
import me.voidinvoid.discordmusic.quiz.QuizPlaylist;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.database.DatabaseSong;
import me.voidinvoid.discordmusic.songs.local.FileSong;
import me.voidinvoid.discordmusic.utils.AlbumArt;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.FormattingUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SongDJ implements SongEventListener, EventListener {

    private TextChannel djChannel, radioChannel;

    private List<DJAction> actions = new ArrayList<>();

    private Map<String, NetworkSong> queueDeletionMessages = new HashMap<>();

    private AudioTrack activeTrack;

    private ScheduledExecutorService executor;
    private ScheduledFuture taskTimer;

    private Message currentMessage;
    private User currentSuggestionUser;

    private int tickerAnimate = 0;

    public SongDJ() {

        djChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.djChat);
        radioChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

        actions.add(new SkipSongAction());
        actions.add(new PauseSongAction());
        actions.add(new RestartSongAction());
        actions.add(new PauseAtEndAction());
        actions.add(new PlayJingleAction());
        actions.add(new ToggleSuggestionsAction());
        actions.add(new PlayAdvertAction());

        executor = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void onEvent(Event ev) { //todo use MessageReactionCallbackManager
        if (ev instanceof MessageReactionAddEvent) {
            MessageReactionAddEvent e = (MessageReactionAddEvent) ev;

            if (e.getUser().isBot()) return;

            if (queueDeletionMessages.containsKey(e.getMessageId())) {
                NetworkSong song = queueDeletionMessages.remove(e.getMessageId());

                song.getQueue().remove(song);

                e.getChannel().deleteMessageById(e.getMessageIdLong()).reason("Network radio song suggestion removed").queue();

                EmbedBuilder embed = new EmbedBuilder().setTitle("Song Queue")
                        .setDescription("Song has been removed from the queue")
                        .setColor(new Color(230, 230, 230))
                        .addField("Name", song.getTrack().getInfo().title, true)
                        .addField("URL", song.getTrack().getInfo().uri, true)
                        .setTimestamp(new Date().toInstant());

                if (song.getSuggestedBy() != null) {
                    embed.setFooter(song.getSuggestedBy().getName(), song.getSuggestedBy().getAvatarUrl());
                }

                AlbumArt.attachAlbumArt(embed, song, djChannel).queue(); //TODO split into another event? and remove via orchestrator

                if (Radio.getInstance().getOrchestrator().areSuggestionsEnabled()) {
                    AlbumArt.attachAlbumArt(embed, song, radioChannel).queue();
                }

                return;
            }

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
                    queueDeletionMessages.remove(id); //remove the ability to cancel this song since it's already playing by now
                    break;
                }
            }
        }

        List<DJAction> availableActions = this.actions.stream().filter(r -> r.shouldShow(track)).collect(Collectors.toList());

        currentSuggestionUser = null;
        MessageAction msg = createMessage(song, track, availableActions, player, timeUntilJingle, false); //send original message and then queue to update every 5 secs

        String tickerTopic = null;

        if (song.getType() == SongType.ADVERTISEMENT) {
            tickerTopic = "🎵 Advertisement";
        } else if (song.getType() != SongType.SONG) {
            tickerTopic = "🎵  **95 Degrees Radio**"; //todo maybe ticker stuff should be isolated
        }

        if (tickerTopic != null) {
            djChannel.getManager().setTopic(tickerTopic).queue();
            radioChannel.getManager().setTopic(tickerTopic).queue();
        }

        msg.queue(m -> {
            currentMessage = m;

            tickerAnimate = 0;

            if (taskTimer != null) taskTimer.cancel(false);

            if (!track.getInfo().isStream) { //TODO ticker otherwise
                taskTimer = executor.scheduleAtFixedRate(() -> {
                    //if (currentMessageFuture != null) currentMessageFuture.cancel(true);

                    //if (tickerAnimate % 2 == 0) //every 2 secs
                    //    currentMessageFuture = SongDJ.this.editMessage(song, track, player, timeUntilJingle, m).submit();

                    if (song.getType() == SongType.SONG) {
                        String title = song instanceof DatabaseSong ? ((DatabaseSong) song).getTitle() : track.getInfo().title;
                        String author = song instanceof DatabaseSong ? ((DatabaseSong) song).getArtist() : track.getInfo().author;

                        String topic = (tickerAnimate >= 6 && currentSuggestionUser != null ? "🎵 Suggested by **" + FormattingUtils.escapeMarkup(currentSuggestionUser.getName()) + "**" : "🎵 **" + title + "** - " + author) + (song.getType() == SongType.SONG ? " - " + FormattingUtils.getFormattedMsTime(track.getPosition()) + " / " + FormattingUtils.getFormattedMsTime(track.getDuration()) : "");

                        tickerAnimate++;
                        if (tickerAnimate >= 12) {
                            tickerAnimate = 0; //todo
                        }

                        KaraokeManager km = Radio.getInstance().getService(KaraokeManager.class);
                        if (km == null || !km.isKaraokeMode()) { //TODO
                            djChannel.getManager().setTopic(topic).queue();
                            radioChannel.getManager().setTopic(topic).queue();
                        }
                    }

                }, 0, 1, TimeUnit.SECONDS);
            } else {
                String title = song instanceof DatabaseSong ? ((DatabaseSong) song).getTitle() : track.getInfo().title;
                String author = song instanceof DatabaseSong ? ((DatabaseSong) song).getArtist() : track.getInfo().author;

                String topic = "🎵 **" + title + "** - " + author + (song.getType() == SongType.SONG ? " - --/--" : "");

                KaraokeManager km = Radio.getInstance().getService(KaraokeManager.class);
                if (km == null || !km.isKaraokeMode()) { //TODO
                    djChannel.getManager().setTopic(topic).queue();
                    radioChannel.getManager().setTopic(topic).queue();
                }
            }

            availableActions.forEach(a -> m.addReaction(a.getEmoji()).queue());
        });
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        if (currentMessage != null) currentMessage.delete().queue();

        currentMessage = null;
        activeTrack = null;

        if (taskTimer != null) taskTimer.cancel(false);
    }

    @Override
    public void onSongPause(boolean paused, Song song, AudioTrack track, AudioPlayer player) {
        if (currentMessage != null) {
            AlbumArt.attachAlbumArtToEdit(new EmbedBuilder(currentMessage.getEmbeds().get(0)).setColor(paused ? Colors.ACCENT_PAUSED : Colors.ACCENT_MAIN), song, currentMessage).queue(m -> currentMessage = m);
        }
    }

    public MessageAction createMessage(Song song, AudioTrack track, List<DJAction> actions, AudioPlayer player, int timeUntilJingle, boolean ended) {

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle((ended ? "Previously " : "") + "Playing " + FormattingUtils.getSongType(track))
                .setColor(ended ? Colors.ACCENT_FINISHED_SONG : player.isPaused() ? Colors.ACCENT_PAUSED : Colors.ACCENT_MAIN)
                .setTimestamp(OffsetDateTime.now());

        if (song instanceof NetworkSong) {
            embed.addField("Title", FormattingUtils.escapeMarkup(track.getInfo().title), true);
            embed.addField("Uploader", FormattingUtils.escapeMarkup(track.getInfo().author), true);
            embed.addField("URL", FormattingUtils.escapeMarkup(song.getFileName()), true);

            NetworkSong ns = (NetworkSong) song;
            if (ns.getSuggestedBy() != null) {
                embed.setFooter(ns.getSuggestedBy().getName(), ns.getSuggestedBy().getAvatarUrl());
                currentSuggestionUser = ns.getSuggestedBy();
            }

        } else if (song instanceof DatabaseSong) {
            DatabaseSong ds = (DatabaseSong) song;

            embed.addField("Title", FormattingUtils.escapeMarkup(ds.getTitle()), true);
            embed.addField("Artist", FormattingUtils.escapeMarkup(ds.getArtist()), true);
            embed.addField("Album Art ID", "(#" + (song.getQueue().getSongMap().indexOf(song) + 1) + ") " + FormattingUtils.escapeMarkup(ds.getFileName()), true);
            embed.addField("MBID", ds.getMbId() == null ? "Unknown" : FormattingUtils.escapeMarkup(ds.getMbId()), true);

        } else if (song instanceof FileSong) {

            if (song.getType() == SongType.SONG) {
                embed.addField("Title", FormattingUtils.escapeMarkup(track.getInfo().title), true);
                embed.addField("Artist", FormattingUtils.escapeMarkup(track.getInfo().author), true);
                embed.addField("File Path", "(#" + (song.getQueue().getSongMap().indexOf(song) + 1) + ") " + FormattingUtils.escapeMarkup(song.getFileName()), true);
            }

        } else {
            embed.addField("Unknown Track Details", "😢", false);
        }

        if (!ended) {
            embed.addField("Next Jingle", timeUntilJingle == 0 ? "After this " + FormattingUtils.getSongType(track) : "After " + (timeUntilJingle + 1) + " more songs", false);
            //embed.addField("Elapsed", track.getInfo().isStream ? "-" : FormattingUtils.getFormattedMsTime(track.getPosition()) + " / " + FormattingUtils.getFormattedMsTime(track.getDuration()), false);
            embed.addField("", actions.stream().map(r -> r.getEmoji() + " " + r.getName()).collect(Collectors.joining("\n")), false);
        }

        return AlbumArt.attachAlbumArt(embed, song, djChannel, true);
    }

    @Override
    public void onSongLoadError(Song song, FriendlyException error) {
        djChannel.sendMessage(new EmbedBuilder()
                .setTitle("Failed to Load Track")
                .setColor(Colors.ACCENT_ERROR)
                .setDescription("Failed to load " + song.getFriendlyName() + ".\nCheck the console for stack trace")
                .addField("Error Message", error.getMessage(), false)
                .setTimestamp(OffsetDateTime.now()).build()).queue();
    }

    @Override
    public void onPlaylistChange(Playlist oldPlaylist, Playlist newPlaylist) {
        djChannel.sendMessage(new EmbedBuilder()
                .setTitle("Playlist")
                .setDescription("Active playlist has been changed")
                .setColor(new Color(230, 230, 230))
                .addField("Name", newPlaylist.getName(), true)
                .setTimestamp(OffsetDateTime.now()).build()).queue();
    }

    @Override
    public void onSuggestionsToggle(boolean enabled, User source) {
        if (source == null) return;

        EmbedBuilder embed = new EmbedBuilder().setTitle("Song Suggestions")
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

        AlbumArt.attachAlbumArt(embed, song, djChannel).queue(m -> {
            m.addReaction("❌").queue();
            queueDeletionMessages.put(m.getId(), song);
        });
    }
}