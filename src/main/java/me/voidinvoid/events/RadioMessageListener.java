package me.voidinvoid.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.Radio;
import me.voidinvoid.songs.FileSong;
import me.voidinvoid.songs.NetworkSong;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongType;
import me.voidinvoid.utils.AlbumArtUtils;
import me.voidinvoid.utils.Colors;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RadioMessageListener implements SongEventListener, EventListener {

    private TextChannel textChannel;
    private Map<String, Song> reactionMessages = new HashMap<>();
    private Song currentSong;

    public RadioMessageListener(TextChannel textChannel) {

        this.textChannel = textChannel;
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (!(song instanceof FileSong)) return;
        if (song.getType() != SongType.SONG) return;

        currentSong = song;

        EmbedBuilder embed = new EmbedBuilder().setTitle("Now Playing")
                .setColor(new Color(230, 230, 230))
                .addField("Title", track.getInfo().title, true)
                .addField(song instanceof NetworkSong ? "Uploader" : "Artist", track.getInfo().author, true)
                .addField("\u200e", "Does this song feature music video specific elements?\n✅ - song is fine\n❌ - music video elements", false)
                .setTimestamp(new Date().toInstant());

        if (song instanceof NetworkSong) {
            NetworkSong ns = (NetworkSong) song;
            if (ns.getSuggestedBy() != null)
                embed.setFooter(ns.getSuggestedBy().getName(), ns.getSuggestedBy().getAvatarUrl());
        }

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue(m -> {
            reactionMessages.put(m.getId(), song);
            m.addReaction("✅").queue();
            m.addReaction("❌").queue();
        });
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        currentSong = null;
    }

    @Override
    public void onNetworkSongQueueError(NetworkSong song, AudioTrack track, User user, NetworkSongError error) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Song Queue")
                .setDescription(error.getErrorMessage())
                .addField("Title", track.getInfo().title, true)
                .addField("URL", track.getInfo().uri, true)
                .setColor(Colors.ACCENT_ERROR)
                .setTimestamp(OffsetDateTime.now());

        if (user != null) {
            embed.setFooter(user.getName(), user.getAvatarUrl());
        }

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue();
    }

    @Override
    public void onNetworkSongQueued(NetworkSong song, AudioTrack track, User user, int queuePosition) {
        if (user == null) return;

        EmbedBuilder embed = new EmbedBuilder().setTitle("Song Queue")
                .setDescription("Added song to the queue")
                .setColor(new Color(230, 230, 230))
                .addField("Title", track.getInfo().title, true)
                .addField("URL", track.getInfo().uri, true)
                .addField("Queue Position", "#" + (queuePosition + 1), false)
                .setTimestamp(OffsetDateTime.now())
                .setFooter(user.getName(), user.getAvatarUrl());

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue();
    }

    @Override
    public void onSuggestionsToggle(boolean enabled, User source) {

    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildMessageReactionAddEvent) {
            GuildMessageReactionAddEvent e = (GuildMessageReactionAddEvent) ev;

            if (e.getUser().isBot()) return;

            if (reactionMessages.containsKey(e.getMessageId())) {
                String reaction = e.getReactionEmote().getName();
                Song song = reactionMessages.get(e.getMessageId());

                boolean res;

                if (reaction.equals("✅")) {
                    res = true;
                    textChannel.sendMessage("✅ Marked `" + song.getLocation() + "` as valid").queue();
                } else if (reaction.equals("❌")) {
                    res = false;
                    textChannel.sendMessage("❌ Marked `" + song.getLocation() + "` as invalid").queue();
                } else {
                    return;
                }

                song.getQueue().getQueue().remove(song);

                if (song.equals(currentSong)) {
                    Radio.instance.getOrchestrator().playNextSong();
                }

                Path file = Paths.get(song.getIdentifier());
                String parent = file.getParent().toString();

                File valid = Paths.get(parent, "valid").toFile();
                File invalid = Paths.get(parent, "invalid").toFile();

                if (!valid.exists()) valid.mkdir();
                if (!invalid.exists()) invalid.mkdir();

                try {
                    Files.move(file, Paths.get(res ? valid.toString() : invalid.toString(), song.getLocation()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                reactionMessages.remove(e.getMessageId());
            }
        }
    }
}
