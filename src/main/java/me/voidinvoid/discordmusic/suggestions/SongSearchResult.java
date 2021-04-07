package me.voidinvoid.discordmusic.suggestions;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.levelling.LevelExtras;
import me.voidinvoid.discordmusic.levelling.LevellingManager;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.QueueOption;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.Emoji;
import me.voidinvoid.discordmusic.utils.Formatting;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.commands.CommandHook;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SongSearchResult {

    private static final int MAX_SEARCH_RESULTS = 5;

    private static final Emote CANCEL_EMOJI = Emoji.CROSS.getEmote();

    private List<AudioTrack> playlist;
    private User user;
    private boolean songsOverLengthLimit;

    public SongSearchResult(AudioPlaylist audioPlaylist, User user, boolean bypassLengthLimit) {

        playlist = audioPlaylist.getTracks();
        this.user = user;

        if (!bypassLengthLimit) {
            var lm = Radio.getInstance().getService(LevellingManager.class);
            var limit = (long) lm.getLatestExtra(user, LevelExtras.MAX_SUGGESTION_LENGTH).getValue();

            int prev = playlist.size();
            playlist = playlist.stream().filter(t -> t.getDuration() <= limit).collect(Collectors.toList());
            if (playlist.size() < prev) songsOverLengthLimit = true;
        }

        if (playlist.size() > MAX_SEARCH_RESULTS) playlist = playlist.subList(0, MAX_SEARCH_RESULTS);
    }

    public List<AudioTrack> getPlaylist() {
        return playlist;
    }

    public int getResultCount() {
        return playlist.size();
    }

    public CompletableFuture<Message> sendMessage(CommandHook hook) {
        int amount = playlist.size();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔎 Song Search")
                .setColor(Colors.ACCENT_SEARCH)
                .setDescription((amount == 1 ? "Here is the top match" : "Here are the top " + amount + " matches") + " from your search\n" + "React with the corresponding number to add to the queue\n\n" + (songsOverLengthLimit ? "⚠ Some results were hidden for being over the length limit\n\n" : ""))
                .setTimestamp(OffsetDateTime.now());

        if (user != null) embed.setFooter(user.getName(), user.getAvatarUrl());

        StringBuilder emojiBuilder = new StringBuilder();

        int i = 0;
        for (AudioTrack t : playlist) {
            System.out.println(t.getIdentifier());
            emojiBuilder.append(Formatting.NUMBER_EMOTES.get(i)).append(" [").append(t.getInfo().title.replaceAll("]", "\\]")).append("](").append("https://youtu.be/").append(t.getIdentifier()).append(") (").append(t.getInfo().author).append(")\n");
            i++;
        }

        embed.addField("", emojiBuilder.toString(), false);

        CompletableFuture<Message> future = hook.editOriginal(embed.build()).submit();

        future.whenComplete((m, e) -> {
            for (int ix = 0; ix < amount; ix++) {
                m.addReaction(Formatting.NUMBER_EMOTES.get(ix)).queue();
            }

            m.addReaction(CANCEL_EMOJI).queue();
        });

        return future;
    }

    public boolean handleReaction(GuildMessageReactionAddEvent e) {
        if (e.getUser().getIdLong() == user.getIdLong()) {
            String reaction = e.getReactionEmote().getName();
            if (Formatting.NUMBER_EMOTES.contains(reaction)) {
                int index = Formatting.NUMBER_EMOTES.indexOf(reaction);

                try {
                    AudioTrack track = playlist.get(index);
                    e.getChannel().deleteMessageById(e.getMessageIdLong()).reason("Search result selected").queue();

                    var song = new NetworkSong(SongType.SONG, track, user, null);
                    Radio.getInstance().getOrchestrator().queueSuggestion(song, ChannelScope.DJ_CHAT.check(e.getChannel()) ? new QueueOption[]{QueueOption.BYPASS_ERRORS} : new QueueOption[]{});

                    return true;
                } catch (Exception ex) {
                    System.out.println("Error handling search reaction event");
                    ex.printStackTrace();
                }
            } else if (e.getReactionEmote().getEmote().equals(CANCEL_EMOJI)) {
                e.getChannel().deleteMessageById(e.getMessageIdLong()).reason("Search result selected").queue();
            }
        }

        return false;
    }
}
