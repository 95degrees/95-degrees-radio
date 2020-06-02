package me.voidinvoid.discordmusic.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.local.FileSong;
import me.voidinvoid.discordmusic.songs.local.LocalSongQueue;
import me.voidinvoid.discordmusic.utils.AlbumArtUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PlaylistTesterListener implements RadioService, SongEventListener, EventListener {

    private static final String VALID_REACTION = "✅", INVALID_REACTION = "❌";

    private TextChannel textChannel;
    private Map<String, Song> reactionMessages = new HashMap<>();
    private Song currentSong;

    @Override
    public void onLoad() {
        this.textChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (!(song instanceof FileSong)) return;
        if (song.getType() != SongType.SONG) return;
        if (!(song.getQueue().getPlaylist() instanceof RadioPlaylist) || !((RadioPlaylist) song.getQueue().getPlaylist()).isTestingMode())
            return;

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
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildMessageReactionAddEvent) {
            GuildMessageReactionAddEvent e = (GuildMessageReactionAddEvent) ev;

            if (e.getUser().isBot()) return;
            if (!reactionMessages.containsKey(e.getMessageId())) return;

            String reaction = e.getReactionEmote().getName();
            Song song = reactionMessages.get(e.getMessageId());

            boolean result;

            if (reaction.equals(VALID_REACTION)) {
                result = true;
                textChannel.sendMessage("✅ Marked `" + song.getInternalName() + "` as valid").queue();
            } else if (reaction.equals(INVALID_REACTION)) {
                result = false;
                textChannel.sendMessage("❌ Marked `" + song.getInternalName() + "` as invalid").queue();
            } else {
                return;
            }

            song.getQueue().getQueue().remove(song);
            song.getQueue().getSongMap().remove(song);

            if (song.equals(currentSong)) {
                Radio.getInstance().getOrchestrator().playNextSong();
            }

            Path parent = ((LocalSongQueue) song.getQueue()).getDirectory(); // /Songs directory

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
                Files.move(Paths.get(song.getLavaIdentifier()), target.resolve(song.getInternalName()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            reactionMessages.remove(e.getMessageId());
        }
    }
}
