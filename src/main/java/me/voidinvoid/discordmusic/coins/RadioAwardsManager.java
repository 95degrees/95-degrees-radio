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
import me.voidinvoid.discordmusic.utils.reactions.ReactionListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * DiscordMusic - 27/04/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class RadioAwardsManager implements RadioService, SongEventListener, EventListener {

    private static final String REWARDS_LOG_PREFIX = ConsoleColor.BLUE_BACKGROUND_BRIGHT + " Awards ";

    private CachedChannel<TextChannel> radioTextChannel;
    private CachedChannel<VoiceChannel> radioVoiceChannel;

    private List<Song> rewardSongs;

    private Map<String, Reward> activeRewards = new HashMap<>();

    @Override
    public String getLogPrefix() {
        return REWARDS_LOG_PREFIX;
    }

    @Override
    public boolean canRun(RadioConfig config) {
        return config.useCoinGain && config.debug;
    }

    @Override
    public void onLoad() {
        radioTextChannel = new CachedChannel<>(RadioConfig.config.channels.radioChat);
        radioVoiceChannel = new CachedChannel<>(RadioConfig.config.channels.voice);

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

    private List<Member> getListening() {
        return radioVoiceChannel.get().getMembers().stream().filter(m -> !m.getUser().isBot()).collect(Collectors.toList());
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (song.getType() != SongType.REWARD) return;

        var reward = new Reward(getListening(), 1.0); //todo

        radioTextChannel.get().sendMessage(
                new EmbedBuilder()
                        .setTitle("Radio Rewards")
                        .setDescription("Thank you for listening to the 95 Degrees Radio! Click the button below to claim a personalised reward.")
                        .setFooter("95 Degrees Radio", Radio.getInstance().getJda().getSelfUser().getEffectiveAvatarUrl())
                        .setColor(Colors.ACCENT_REWARD)
                        .build()).queue(m -> {

                    activeRewards.put(m.getId(), reward);
                    new ReactionListener(m, true).add("🎈", ev -> giveReward(ev.getMember(), reward));

                    m.delete().queueAfter(3, TimeUnit.MINUTES); //todo make configurable?
                }
        );
    }

    public void giveReward(Member member, Reward reward) {
        member.getUser().openPrivateChannel().queue(c -> {

            EmbedBuilder msg = new EmbedBuilder()
                    .setTitle("Radio Rewards")
                    .setFooter("95 Degrees Radio", Radio.getInstance().getJda().getSelfUser().getEffectiveAvatarUrl())
                    .setColor(Colors.ACCENT_REWARD);

            if (!reward.isEligible(member)) {
                msg.setDescription("You are ineligible to claim this reward :(").setColor(Colors.ACCENT_ERROR);
            } else if (reward.hasAlreadyClaimed(member)) {
                msg.setDescription("You have already claimed this reward!").setColor(Colors.ACCENT_ERROR);
            } else {
                reward.markClaimed(member);
                msg.setDescription("You have claimed a reward of [REWARD HERE]!").setColor(Colors.ACCENT_REWARD);
            }

            c.sendMessage(msg.build()).queue();
        }, e -> {
            log("Warning: couldn't open direct message channel to " + member);
        });
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildMessageDeleteEvent) {
            var e = (GuildMessageDeleteEvent) ev;

            activeRewards.remove(e.getMessageId());
        }
    }
}
