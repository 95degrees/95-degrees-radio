package me.voidinvoid.discordmusic.suggestions;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.HashMap;
import java.util.Map;

public class SongSuggestionManager implements EventListener {

    private Map<String, SongSearchResult> searches = new HashMap<>();

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildMessageReceivedEvent) {
            GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) ev;

            if (!ChannelScope.RADIO_AND_DJ_CHAT.check(e.getChannel())) return;
            if (e.getAuthor().isBot()) return;

            addSuggestion(e.getMessage().getContentRaw(), e.getMessage(), e.getChannel(), e.getMember(), false, SuggestionQueueMode.NORMAL);
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

    public void addSuggestion(String identifier, Message suggestionMessage, TextChannel channel, Member member, boolean notifyOnFailure, SuggestionQueueMode queueMode) {
        Radio.instance.getOrchestrator().getAudioManager().loadItem(identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                Radio.instance.getOrchestrator().addNetworkTrack(member, track, channel == null || ChannelScope.DJ_CHAT.check(channel), queueMode == SuggestionQueueMode.PLAY_INSTANTLY, queueMode == SuggestionQueueMode.PUSH_TO_START);
                if (suggestionMessage != null) suggestionMessage.delete().reason("Song suggestion URL").queue();
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
                } else {
                    search.sendMessage(channel).whenComplete((m, e) -> searches.put(m.getId(), search));
                }

                if (suggestionMessage != null) suggestionMessage.delete().reason("Song suggestion URL").queue();
            }

            @Override
            public void noMatches() {
                if (notifyOnFailure) {
                    channel.sendMessage(new EmbedBuilder()
                            .setTitle("Search Failure")
                            .setColor(Colors.ACCENT_ERROR)
                            .setDescription("No song results found for your search")
                            .build()).queue();

                    if (suggestionMessage != null) suggestionMessage.delete().reason("Song suggestion URL").queue();
                }
            }

            @Override
            public void loadFailed(FriendlyException e) {
            }
        });
    }
}
