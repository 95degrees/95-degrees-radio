package me.voidinvoid.discordmusic.coins;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.activity.ListeningContext;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.economy.EconomyManager;
import me.voidinvoid.discordmusic.economy.Transaction;
import me.voidinvoid.discordmusic.economy.TransactionType;
import me.voidinvoid.discordmusic.events.RadioEventListener;
import me.voidinvoid.discordmusic.interactions.ButtonManager;
import me.voidinvoid.discordmusic.levelling.ListeningTrackerManager;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import me.voidinvoid.discordmusic.utils.Emoji;
import me.voidinvoid.discordmusic.utils.Service;
import me.voidinvoid.discordmusic.utils.cache.CachedChannel;
import me.voidinvoid.discordmusic.utils.reactions.ReactionListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.button.ButtonStyle;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * DiscordMusic - 27/04/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class RadioAwardsManager implements RadioService, RadioEventListener, EventListener {

    private static final int BASE_SONGS_PER_REWARD = 5; //both requirements must be met
    private static final long BASE_DURATION_BETWEEN_REWARDS = 15 * 60 * 1000;

    private static final String REWARDS_LOG_PREFIX = ConsoleColor.BLUE_BACKGROUND_BRIGHT + " Awards ";

    private CachedChannel<TextChannel> radioTextChannel;
    private CachedChannel<VoiceChannel> radioVoiceChannel;

    private List<Song> rewardSongs;

    private Map<String, Reward> activeRewards = new HashMap<>();

    private long nextRewardTimeMinimum = System.currentTimeMillis() + BASE_DURATION_BETWEEN_REWARDS;
    private int songsUntilReward = BASE_SONGS_PER_REWARD;

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
        radioVoiceChannel = new CachedChannel<>(RadioConfig.config.channels.voice);

        rewardSongs = new ArrayList<>();

        for (var identifier : RadioConfig.config.locations.rewardIdentifiers) {
            Radio.getInstance().getOrchestrator().createNetworkSong(SongType.REWARD, identifier).thenAccept(n -> rewardSongs.add(n));
        }
    }

    public void pushAward() {
        log("Attempting to queue reward...");

        if (rewardSongs.isEmpty()) {
            log("Warning: tried to queue a reward song but none are available!");
            return;
        }

        List<Song> awaitingSongs = Radio.getInstance().getOrchestrator().getAwaitingSpecialSongs();
        if (awaitingSongs.stream().noneMatch(s -> s.getType() == SongType.REWARD)) {
            log("Added reward song to special queue");
            awaitingSongs.add(rewardSongs.get(ThreadLocalRandom.current().nextInt(rewardSongs.size()))); //random each time

            Service.of(RPCSocketManager.class).updateUpcomingEvents();
        }
    }

    @Override
    public void onSongStart(Song song, AudioTrack track, AudioPlayer player, int timeUntilJingle) {
        if (song.getType() == SongType.SONG) {
            songsUntilReward--;
            log("Songs until reward: " + songsUntilReward + ", next reward time: " + nextRewardTimeMinimum + " (" + (nextRewardTimeMinimum - System.currentTimeMillis()) + "ms)");
            if (songsUntilReward <= 0 && System.currentTimeMillis() > nextRewardTimeMinimum) {
                log("Pushing award!");
                pushAward();
            }
            return;
        }

        if (song.getType() != SongType.REWARD) {
            return;
        }

        nextRewardTimeMinimum = System.currentTimeMillis() + BASE_DURATION_BETWEEN_REWARDS;
        songsUntilReward = BASE_SONGS_PER_REWARD;

        var reward = new Reward(ListeningContext.ALL.getListeners(), Radio.getInstance().getOrchestrator().getActivePlaylist().getCoinMultiplier());

        ButtonManager.applyButtons(radioTextChannel.get().sendMessage(
                new EmbedBuilder()
                        .setTitle("Radio Rewards")
                        .setDescription("Thank you for listening to the 95 Degrees Radio! Click the button below to claim a reward.")
                        .setFooter("95 Degrees Radio", Radio.getInstance().getJda().getSelfUser().getEffectiveAvatarUrl())
                        .setColor(Colors.ACCENT_REWARD)
                        .build()), ButtonManager.of(ButtonStyle.SUCCESS, "Claim reward!", e -> giveReward(e.getEvent(), reward))).queue(m -> {

                    activeRewards.put(m.getId(), reward);

                    m.delete().submitAfter(1, TimeUnit.MINUTES).thenRun(() -> Service.of(ListeningTrackerManager.class).resetCurrentListeningDurations());
                }
        );
    }

    public void giveReward(ButtonClickEvent event, Reward reward) {
        if (event.getMember() == null) {
            return;
        }

        var member = event.getMember();

        EmbedBuilder msg = new EmbedBuilder()
                .setTitle("Radio Rewards")
                .setColor(Colors.ACCENT_REWARD);

        if (!reward.isEligible(member)) {
            msg.setDescription("You are ineligible to claim this reward :(").setColor(Colors.ACCENT_ERROR);
        } else if (reward.hasAlreadyClaimed(member)) {
            msg.setDescription("You have already claimed this reward!").setColor(Colors.ACCENT_ERROR);
        } else {
            reward.markClaimed(member);
            var amount = Service.of(ListeningTrackerManager.class).getCurrentListeningDuration(member);

            Service.of(EconomyManager.class).makeTransaction(member, new Transaction(TransactionType.RADIO, amount));

            msg.setDescription("You have claimed a reward of " + Emoji.DEGREECOIN + amount + "!").setColor(Colors.ACCENT_REWARD);
        }

        event.deferReply(true).addEmbeds(msg.build()).queue();
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildMessageDeleteEvent) {
            var e = (GuildMessageDeleteEvent) ev;

            activeRewards.remove(e.getMessageId());
        }
    }
}
