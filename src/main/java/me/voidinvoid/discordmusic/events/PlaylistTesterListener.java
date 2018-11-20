package me.voidinvoid.discordmusic.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.FileSong;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongPlaylist;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.AlbumArtUtils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PlaylistTesterListener implements SongEventListener, EventListener {

    private static final String VALID_REACTION = "✅", INVALID_REACTION = "❌";

    private TextChannel textChannel;
    private Map<String, Song> reactionMessages = new HashMap<>();
    private Song currentSong;

    public PlaylistTesterListener(TextChannel textChannel) {

        this.textChannel = textChannel;
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (!(song instanceof FileSong)) return;
        if (song.getType() != SongType.SONG) return;
        if (!(song.getQueue().getPlaylist() instanceof SongPlaylist) || !((SongPlaylist) song.getQueue().getPlaylist()).isTestingMode()) return;

        currentSong = song;

        EmbedBuilder embed = new EmbedBuilder().setTitle("Now Playing")
                .setColor(new Color(230, 230, 230))
                .setDescription("Does this song feature music video specific elements?\n" + VALID_REACTION + " - song is valid\n" + INVALID_REACTION + " - song is invalid")
                .setTimestamp(new Date().toInstant());

        AlbumArtUtils.attachAlbumArt(embed, song, textChannel).queue(m -> {
            reactionMessages.put(m.getId(), song);

            m.addReaction(VALID_REACTION).queue();
            m.addReaction(INVALID_REACTION).queue();
        });
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        currentSong = null;
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildMessageReactionAddEvent) {
            GuildMessageReactionAddEvent e = (GuildMessageReactionAddEvent) ev;

            if (e.getUser().isBot()) return;
            if (!reactionMessages.containsKey(e.getMessageId())) return;

            String reaction = e.getReactionEmote().getName();
            Song song = reactionMessages.get(e.getMessageId());

            boolean result;

            if (reaction.equals(VALID_REACTION)) {
                result = true;
                textChannel.sendMessage("✅ Marked `" + song.getLocation() + "` as valid").queue();
            } else if (reaction.equals(INVALID_REACTION)) {
                result = false;
                textChannel.sendMessage("❌ Marked `" + song.getLocation() + "` as invalid").queue();
            } else {
                return;
            }

            song.getQueue().getQueue().remove(song);
            song.getQueue().getSongMap().remove(song);

            if (song.equals(currentSong)) {
                Radio.instance.getOrchestrator().playNextSong();
            }

            Path parent = song.getQueue().getDirectory().toPath(); // /Songs directory

            Path validSongs = parent.resolve("valid");
            Path invalidSongs = parent.resolve("invalid");

            if (Files.notExists(validSongs)) {
                try {
                    Files.createDirectory(validSongs);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            if (Files.notExists(invalidSongs)) {
                try {
                    Files.createDirectory(invalidSongs);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            Path target = result ? validSongs : invalidSongs;

            try {
                Files.move(Paths.get(song.getIdentifier()), target.resolve(song.getLocation()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            reactionMessages.remove(e.getMessageId());
        }
    }
}
