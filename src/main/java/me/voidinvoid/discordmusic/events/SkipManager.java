package me.voidinvoid.discordmusic.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.activity.ListeningContext;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandData;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.Emoji;
import me.voidinvoid.discordmusic.utils.cache.CachedChannel;
import me.voidinvoid.discordmusic.utils.cache.CachedMember;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SkipManager implements RadioService, RadioEventListener, EventListener {

    private int skipThreshold;

    private Set<String> skipRequests = new HashSet<>();
    private String skipStatus;

    private CachedChannel<TextChannel> radioTextChannel;

    @Override
    public void onLoad() {
        radioTextChannel = new CachedChannel<>(RadioConfig.config.channels.radioChat);
    }

    private void calculateSkipThreshold() {
        var members = ListeningContext.ALL.getListeners();
        skipThreshold = (int) Math.ceil((members.size() - 1) * 0.5) + 1;
        //skipThreshold = 10;

        log("Current song skip threshold: " + skipThreshold);

        skipRequests.removeIf(u -> !members.contains(new CachedMember(u)));

        if (skipRequests.size() >= skipThreshold && skipRequests.size() > 0) {
            Radio.getInstance().getOrchestrator().playNextSong(false, true, true);
        }

        skipStatus = null;
    }

    public int getSkipThreshold() {
        return skipThreshold;
    }

    public int addSkipRequest(User user, SlashCommandData interaction) {

        var skipRequested = true;

        if (!skipRequests.add(user.getId())) {
            skipRequests.remove(user.getId()); //un-toggle
            skipRequested = false;
        }

        skipStatus = null;

        var req = skipRequests.size();
        log("skip requests: " + skipRequests.size() + "/ " + skipThreshold);

        var status = generateSkipStatus();
        var embed = new EmbedBuilder().setColor(Colors.ACCENT_SKIP_REQUESTS).setTitle("⏩ Skip request " + (skipRequested ? "added" : "removed"));

        if (status != null) {
            embed.addField("Skip Requests", status, false);
        }

        if (interaction != null) {
            interaction.getEvent().replyEmbeds(embed.build()).queue(c -> c.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
        } else {
            radioTextChannel.get().sendMessage(embed.build()).queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
        }

        calculateSkipThreshold();

        return req;
    }

    public String generateSkipStatus() {
        if (skipStatus != null) {
            return skipStatus;
        }

        if (skipRequests.isEmpty()) {
            return null;
        }

        return skipStatus = skipRequests.stream().map(id -> {
            var u = Radio.getInstance().getJda().getUserById(id);
            if (u == null) {
                return Emoji.TICK.toString();
            }

            var e = Emoji.getOrCreateUserEmoji(u, skipRequests);
            return e == null ? Emoji.TICK.toString() : e.toString();
        }).collect(Collectors.joining(" "))
                + (" " + Emoji.DIVIDER.toString()).repeat(Math.max(0, skipThreshold - skipRequests.size())) + " **" + skipRequests.size() + "/" + skipThreshold + "** needed to skip";
    }

    public void removeSkipRequest(User user) {
        skipRequests.remove(user.getId());
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildVoiceUpdateEvent) {
            calculateSkipThreshold();
        }
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        skipRequests.clear();
        calculateSkipThreshold();
    }

    @Override
    public void onSongEnd(Song song, AudioTrack track) {
        skipRequests.clear();
        calculateSkipThreshold();
    }
}
