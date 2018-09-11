package me.voidinvoid.suggestions;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.Radio;
import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.config.RadioConfig;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SongSearchResult {

    private static final int MAX_SEARCH_RESULTS = 5;

    private static final List<String> NUMBER_EMOJIS = Arrays.asList(new String(new char[]{49, 8419}), new String(new char[]{50, 8419}), new String(new char[]{51, 8419}), new String(new char[]{52, 8419}), new String(new char[]{53, 8419}), new String(new char[]{54, 8419}), new String(new char[]{55, 8419}), new String(new char[]{56, 8419}), new String(new char[]{57, 8419}), "🔟");
    private static final String CANCEL_EMOJI = "❌";

    private List<AudioTrack> playlist;
    private User user;
    private boolean songsOverLengthLimit;

    public SongSearchResult(AudioPlaylist audioPlaylist, User user, boolean bypassLengthLimit) {

        playlist = audioPlaylist.getTracks();
        this.user = user;

        if (!bypassLengthLimit) {
            int prev = playlist.size();
            playlist = playlist.stream().filter(t -> t.getDuration() <= SongOrchestrator.MAX_SONG_LENGTH).collect(Collectors.toList());
            if (playlist.size() < prev) songsOverLengthLimit = true;
        }

        if (playlist.size() > MAX_SEARCH_RESULTS) playlist = playlist.subList(0, MAX_SEARCH_RESULTS);
    }

    public int getResultCount() {
        return playlist.size();
    }

    public CompletableFuture<Message> sendMessage(TextChannel channel) {
        int amount = playlist.size();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Song Search")
                .setColor(new Color(240, 150, 60))
                .setDescription((amount == 1 ? "Here is the top match" : "Here are the top " + amount + " matches") + " from your search\n" + "React with the corresponding number to add to the queue\n\n" + (songsOverLengthLimit ? "⚠ Some results were omitted for being over the length limit\n\n" : ""))
                .setTimestamp(OffsetDateTime.now());

        if (user != null) embed.setFooter(user.getName(), user.getAvatarUrl());

        StringBuilder emojiBuilder = new StringBuilder();

        int i = 0;
        for (AudioTrack t : playlist) {
            emojiBuilder.append(NUMBER_EMOJIS.get(i)).append(" ").append(t.getInfo().title).append(" (").append(t.getInfo().author).append(")\n");
            i++;
        }

        embed.addField("", emojiBuilder.toString(), false);

        CompletableFuture<Message> future = CompletableFuture.supplyAsync(() -> channel.sendMessage(embed.build()).complete());

        future.whenComplete((m, e) -> {
            for (int ix = 0; ix < amount; ix++) {
                m.addReaction(NUMBER_EMOJIS.get(ix)).queue();
            }

            m.addReaction(CANCEL_EMOJI).queue();
        });

        return future;
    }

    public boolean handleReaction(GuildMessageReactionAddEvent e) {
        if (e.getUser().getIdLong() == user.getIdLong()) {
            String reaction = e.getReaction().getReactionEmote().getName();
            if (NUMBER_EMOJIS.contains(reaction)) {
                int index = NUMBER_EMOJIS.indexOf(reaction);

                try {
                    AudioTrack track = playlist.get(index);
                    e.getChannel().deleteMessageById(e.getMessageIdLong()).reason("Search result selected").queue();

                    Radio.instance.getOrchestrator().addNetworkTrack(e.getUser(), track, e.getChannel().getId().equals(RadioConfig.config.channels.djChat), false, false);
                    return true;
                } catch (Exception ex) {
                    System.out.println("Error handling search reaction event");
                    ex.printStackTrace();
                }
            } else if (reaction.equals(CANCEL_EMOJI)) {
                e.getChannel().deleteMessageById(e.getMessageIdLong()).reason("Search result selected").queue();
            }
        }

        return false;
    }
}
