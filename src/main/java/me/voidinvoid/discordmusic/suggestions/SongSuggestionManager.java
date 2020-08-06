package me.voidinvoid.discordmusic.suggestions;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.QueueOption;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SongSuggestionManager implements RadioService, EventListener {

    private Map<String, SongSearchResult> searches = new HashMap<>();

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildMessageReceivedEvent) {
            GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) ev;

            if (!ChannelScope.RADIO_AND_DJ_CHAT.check(e.getChannel())) return;
            if (e.getAuthor().isBot()) return;

            addSuggestion(e.getMessage().getContentRaw(), e.getMessage(), e.getChannel(), e.getMember(), false, true, SuggestionQueueMode.NORMAL);
        } else if (ev instanceof GuildMessageReactionAddEvent) {
            GuildMessageReactionAddEvent e = (GuildMessageReactionAddEvent) ev;

            SongSearchResult search = searches.get(e.getMessageId());
            if (search != null) {
                if (search.handleReaction(e)) {
                    searches.remove(e.getMessageId());
                }
            }
        }
    }

    public CompletableFuture<Boolean> addSuggestion(String identifier, Message suggestionMessage, TextChannel channel, Member member, boolean notifyOnFailure, boolean autoSelectFirstEntry, SuggestionQueueMode queueMode) {

        var future = new CompletableFuture<Boolean>(); //returns true if we locate the song, NOT if it was queued successfully, mainly for rpc feedback

        var sm = Service.of(SpotifyManager.class);
        var find = sm.findTrack(identifier);

        if (find != null) { //if this is a spotify uri
            find.thenAccept(t -> {
                if (t != null) {
                    sm.fetchLavaTrack(t).thenAccept(s -> {
                        if (s != null) {
                            if (suggestionMessage != null) {
                                suggestionMessage.delete().reason("Song suggestion URL").queue();
                            }

                            var song = new NetworkSong(SongType.SONG, s, member == null ? null : member.getUser());
                            song.setSpotifyTrack(t);

                            Radio.getInstance().getOrchestrator().queueSuggestion(song);

                            future.complete(true);
                        } else {
                            future.complete(false);
                        }
                    });
                }
            });

            return future;
        }

        var opts = new ArrayList<QueueOption>();

        if (channel == null || ChannelScope.DJ_CHAT.check(channel)) {
            opts.add(QueueOption.BYPASS_ERRORS);
        }

        if (queueMode == SuggestionQueueMode.PLAY_INSTANTLY) {
            opts.add(QueueOption.PLAY_INSTANTLY);
        } else if (queueMode == SuggestionQueueMode.PUSH_TO_START) {
            opts.add(QueueOption.PUSH_TO_START);
        }

        Radio.getInstance().getOrchestrator().getAudioManager().loadItem(identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                var song = new NetworkSong(SongType.SONG, track, member == null ? null : member.getUser());
                Radio.getInstance().getOrchestrator().queueSuggestion(song, opts.toArray(new QueueOption[]{}));

                if (suggestionMessage != null) {
                    suggestionMessage.delete().reason("Song suggestion URL").queue();
                }

                future.complete(true);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (channel == null) return;

                SongSearchResult search = new SongSearchResult(playlist, member.getUser(), ChannelScope.DJ_CHAT.check(channel));
                if (search.getResultCount() == 0) {
                    if (notifyOnFailure) {
                        channel.sendMessage(new EmbedBuilder()
                                .setTitle("Search Failure")
                                .setColor(Colors.ACCENT_ERROR)
                                .setDescription("No song results found for your search. Some songs were found but were too long to be included")
                                .build()).queue();
                    }

                    future.complete(false);
                } else if (autoSelectFirstEntry) {
                    var song = new NetworkSong(SongType.SONG, search.getPlaylist().get(0), member.getUser());
                    Radio.getInstance().getOrchestrator().queueSuggestion(song, opts.toArray(new QueueOption[]{}));
                    future.complete(true);
                } else {
                    search.sendMessage(channel).whenComplete((m, e) -> searches.put(m.getId(), search));
                    future.complete(true);
                }

                if (suggestionMessage != null) {
                    suggestionMessage.delete().reason("Song suggestion URL").queue();
                }
            }

            @Override
            public void noMatches() {
                if (notifyOnFailure && channel != null) {
                    channel.sendMessage(new EmbedBuilder()
                            .setTitle("Search Failure")
                            .setColor(Colors.ACCENT_ERROR)
                            .setDescription("No song results found for your search")
                            .build()).queue();

                    if (suggestionMessage != null) {
                        suggestionMessage.delete().reason("Song suggestion URL").queue();
                    }
                }

                future.complete(false);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                future.complete(false);
            }
        });

        return future;
    }
}
