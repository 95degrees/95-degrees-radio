package me.voidinvoid;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import me.voidinvoid.songs.FileSong;
import me.voidinvoid.songs.NetworkSong;
import me.voidinvoid.songs.Song;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.requests.RequestFuture;

import java.awt.*;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.Date;

import static me.voidinvoid.Utils.*;

public class SongDJ extends AudioEventAdapter implements EventListener {

    private static final String SKIP_EMOTE = "⏩", PAUSE_EMOTE = "⏯", RESTART_EMOTE = "⏪", JINGLE_EMOTE = "🎹", TOGGLE_SUGGESTIONS_EMOTE = "📔";

    private TextChannel channel;

    private AudioTrack activeTrack;

    //private Song currentSong;
    private boolean currentJingle;
    private int nextJingle;

    private Message currentMessage;

    private AudioPlayer player;

    //private int songNumber = 0;

    private SongOrchestrator orchestrator;

    public SongDJ(SongOrchestrator orchestrator, TextChannel channel, AudioPlayer player) {
        this.orchestrator = orchestrator;
        this.channel = channel;
        this.player = player;

        channel.getJDA().addEventListener(this);
    }

    public void onNewSong(AudioTrack activeTrack, boolean isJingle, int nextJingle) {
        //currentSong = song;
        this.activeTrack = activeTrack;
        currentJingle = isJingle;
        this.nextJingle = nextJingle;

        //songNumber++;
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof MessageReactionAddEvent) {
            MessageReactionAddEvent e = (MessageReactionAddEvent) ev;
            if (e.getUser().isBot()) return;
            if (currentMessage == null) return;
            if (!e.getMessageId().equals(currentMessage.getId())) return;

            String emote = e.getReaction().getReactionEmote().getName();

            switch (emote) {
                case SKIP_EMOTE:
                    player.setPaused(false);
                    player.stopTrack();
                    orchestrator.playNextSong();
                    System.out.println("Stopping track");
                    break;
                case PAUSE_EMOTE:
                    player.setPaused(!player.isPaused());
                    System.out.println("Playing/pausing track");
                    break;
                case JINGLE_EMOTE:
                    player.setPaused(false);
                    orchestrator.setTimeUntilJingle(-1);
                    player.stopTrack();
                    orchestrator.playNextSong();
                    System.out.println("Skipping track and playing jingle");
                    break;
                case RESTART_EMOTE:
                    if (!player.getPlayingTrack().isSeekable()) break;
                    player.getPlayingTrack().setPosition(0);
                    System.out.println("Restarting song from beginning");
                    break;
                case TOGGLE_SUGGESTIONS_EMOTE:
                    boolean enabled = orchestrator.setSuggestionsEnabled(!orchestrator.areSuggestionsEnabled());
                    channel.sendMessage(new EmbedBuilder().setTitle("Song Suggestions")
                            .setDescription("Song suggestions " + (enabled ? "enabled" : "disabled"))
                            .setFooter(e.getUser().getName(), e.getUser().getAvatarUrl())
                            .setTimestamp(OffsetDateTime.now())
                            .build()).queue();
                    break;
            }

            e.getReaction().removeReaction(e.getUser()).queue();
        }
    }

    public void beginPlayerTimer(final AudioPlayer player, final AudioTrack track, final File albumArt) {
        Thread t = new Thread(() -> {
            long lastPos = track.getPosition();
            int matchCount = 0;
            while (track == activeTrack) {
                try {

                    RequestFuture future = currentMessage.editMessage(createMessage(player, track, albumArt)).submit();
                    Thread.sleep(2000);

                    if (!RadioConfig.config.debug) {
                        if (track.getPosition() == lastPos && !player.isPaused() && !track.getInfo().isStream) { //stuck??
                            matchCount++;
                        } else {
                            matchCount = 0;
                        }
                        if (matchCount >= 5) {
                            System.out.println("WARNING: track frozen so fail-safe skipped track");
                            SongOrchestrator.instance.playNextSong(); //TRACK FROZEN
                        } else {
                            lastPos = track.getPosition();
                        }
                    }
                    future.cancel(true); //cancel since we've already (probably) got newer data
                } catch (Exception ignored) {
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public MessageEmbed createMessage(AudioPlayer player, AudioTrack track, File albumArt) {
        Song song = activeTrack.getUserData(Song.class);

        EmbedBuilder embed = new EmbedBuilder().setTitle("Playing " + (track.getInfo().isStream ? "Stream" : ((currentJingle) ? "Jingle" : "Song")))
                .setColor(player.isPaused() ? Color.RED : new Color(230, 230, 230))
                .setThumbnail("attachment://" + albumArt.getName())
                .addField("Title" + (song instanceof FileSong ? " (#" + (song.getQueue().getSongMap().indexOf(song) + 1) + ")" : ""), track.getInfo().title, true)
                .addField(song instanceof NetworkSong ? "Uploader" : "Artist", Utils.escape(track.getInfo().author), true)
                .addField(song instanceof NetworkSong ? "URL" : "File", Utils.escape(song.getLocation()), false)
                .addField("Next Jingle", nextJingle == 0 ? track.getInfo().isStream ? "After this stream" : "After this song (in " + getFormattedMsTimeLabelled(track.getDuration() - track.getPosition()) + ")" : "After " + (nextJingle + 1) + " more songs", false)
                .addField("Elapsed", track.getInfo().isStream ? "-" : getFormattedMsTime(track.getPosition()) + " / " + getFormattedMsTime(track.getDuration()), false)
                .addField("",
                        SKIP_EMOTE + " Skip\n" +
                                PAUSE_EMOTE + (player.isPaused() ? " Play\n" : " Pause\n") +
                                (track.isSeekable() ? RESTART_EMOTE + " Restart Song\n" : "") +
                                JINGLE_EMOTE + " Skip and Play Jingle\n" +
                                TOGGLE_SUGGESTIONS_EMOTE + " Toggle Song Suggestions", false)
                .setTimestamp(new Date().toInstant());

        if (song instanceof NetworkSong) {
            NetworkSong ns = (NetworkSong) song;
            if (ns.getSuggestedBy() != null)
                embed.setFooter(ns.getSuggestedBy().getName(), ns.getSuggestedBy().getAvatarUrl());
        }

        return embed.build();
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        //jda.getPresence().setIdle(true);
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        //jda.getPresence().setIdle(false);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        File albumArt = track.getUserData(Song.class).getAlbumArtFile();
        Message m = channel.sendFile(albumArt).embed(createMessage(player, track, albumArt)).complete(); //uses blocking because it's important the message is sent asap

        m.addReaction(SKIP_EMOTE).queue();
        m.addReaction(PAUSE_EMOTE).queue();
        if (track.isSeekable()) m.addReaction(RESTART_EMOTE).queue();
        m.addReaction(JINGLE_EMOTE).queue();
        m.addReaction(TOGGLE_SUGGESTIONS_EMOTE).queue();
        currentMessage = m;

        if (!track.getInfo().isStream) beginPlayerTimer(player, track, albumArt);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {

    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        //playNextSong(false, false);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        //playNextSong();
    }
}