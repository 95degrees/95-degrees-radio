package me.voidinvoid.discordmusic.coins;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import me.voidinvoid.discordmusic.utils.Service;
import me.voidinvoid.discordmusic.utils.cache.CachedChannel;
import me.voidinvoid.discordmusic.utils.reactions.MessageReactionListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * DiscordMusic - 27/04/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class RadioAwardsManager implements RadioService, SongEventListener {

    private static final String REWARDS_LOG_PREFIX = ConsoleColor.BLUE_BACKGROUND_BRIGHT + " Awards ";

    private CachedChannel<TextChannel> radioTextChannel;
    private List<Song> rewardSongs;

    @Override
    public String getLogPrefix() {
        return REWARDS_LOG_PREFIX;
    }

    @Override
    public boolean canRun(RadioConfig config) {
        return config.useCoinGain;
    }

    @Override
    public void onLoad() {
        radioTextChannel = new CachedChannel<>(RadioConfig.config.channels.radioChat);

        rewardSongs = new ArrayList<>();

        for (var identifier : RadioConfig.config.locations.rewardIdentifiers) {
            Radio.getInstance().getOrchestrator().createNetworkTrack(SongType.REWARD, identifier, n -> rewardSongs.add(n));
        }
    }

    public void pushAward() {
        if (rewardSongs.isEmpty()) {
            log("Warning: tried to queue a reward song but none are available!");
            return;
        }

        List<Song> awaitingSongs = Radio.getInstance().getOrchestrator().getAwaitingSpecialSongs();
        if (awaitingSongs.stream().noneMatch(s -> s.getType() == SongType.REWARD)) {
            awaitingSongs.add(rewardSongs.get(ThreadLocalRandom.current().nextInt(rewardSongs.size()))); //random each time

            Service.of(RPCSocketManager.class).updateUpcomingEvents();
        }
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (song.getType() != SongType.REWARD) return;

        radioTextChannel.get().sendMessage(
                new EmbedBuilder()
                        .setTitle("Radio Rewards")
                        .setDescription("Thank you for listening to the 95 Degrees Radio! Click the button below to claim a personalised reward.")
                        .setFooter("95 Degrees Radio", Radio.getInstance().getJda().getSelfUser().getEffectiveAvatarUrl())
                        .setColor(Colors.ACCENT_REWARD)
                        .build()).queue(m -> new MessageReactionListener(m)
                .on("🎈", this::giveReward)
        );
    }

    public void giveReward(Member member) {
        member.getUser().openPrivateChannel().queue(c -> {

        }, e -> {
            log("Warning: couldn't open direct message channel to " + member);
        });
    }
}
