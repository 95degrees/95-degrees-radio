package me.voidinvoid.discordmusic.restream;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.audio.AudioPlayerSendHandler;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.levelling.ListeningTrackerManager;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;

/**
 * DiscordMusic - 29/04/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class RadioRestreamManager implements RadioService {

    private static final String RESTREAM_LOG_PREFIX = ConsoleColor.RED_BACKGROUND_BRIGHT + " Restream ";

    private JDA jda;
    private boolean ready = false;

    private String voiceChannel;

    @Override
    public String getLogPrefix() {
        return RESTREAM_LOG_PREFIX;
    }

    @Override
    public boolean canRun(RadioConfig config) {
        return config.restreamBotToken != null;
    }

    @Override
    public void onLoad() {
        if (!isConnected()) { //don't bother reconnecting on service reload
            connect();
        }
    }

    public boolean isConnected() {
        return jda != null && ready;
    }

    public void connect() {
        if (jda != null) return;

        try {
            jda = JDABuilder.createDefault(RadioConfig.config.restreamBotToken)
                    .addEventListeners((EventListener) ev -> {
                        if (ev instanceof ReadyEvent) {
                            ready = true;
                        }
                    })
                    .setActivity(null)
                    .disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.EMOTE, CacheFlag.MEMBER_OVERRIDES)
                    .disableIntents(GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS & ~GatewayIntent.GUILD_VOICE_STATES.getRawValue())) //save on resources
                    .setStatus(OnlineStatus.INVISIBLE) //don't show up on sidebar
                    .build();

        } catch (LoginException e) {
            log(ConsoleColor.RED + "Restream Discord login failure!" + ConsoleColor.RESET);
            e.printStackTrace();
        }
    }

    public void joinVoiceChannel(String voiceChannel) { //use string because we want a voice channel for our jda instance

        if (!isConnected()) {
            connect();
        }

        var vc = jda.getVoiceChannelById(voiceChannel);

        if (vc == null) {
            log("Warning: attempted to join null voice channel " + voiceChannel);
            return;
        }

        var guild = vc.getGuild();
        var audioManager = guild.getAudioManager();

        audioManager.setSendingHandler(new AudioPlayerSendHandler.RestreamAudioPlayerSendHandler(Radio.getInstance().getOrchestrator().getAudioSendHandler()));
        audioManager.openAudioConnection(vc);

        var radioJdaVoice = Radio.getInstance().getJda().getVoiceChannelById(voiceChannel);

        if (radioJdaVoice != null) {
            for (var listener : radioJdaVoice.getMembers()) {
                Service.of(ListeningTrackerManager.class).trackIfEligible(listener.getUser(), radioJdaVoice, listener.getVoiceState() == null || listener.getVoiceState().isDeafened());
            }
        }

        this.voiceChannel = voiceChannel;
    }

    public boolean leaveVoice(String guild) { //use string because we want a guild for our jda instance

        var g = jda.getGuildById(guild);

        if (g == null) {
            log("Warning: attempted to find null guild " + guild);
            return false;
        }

        if (this.voiceChannel == null) {
            return false;
        }

        g.getAudioManager().closeAudioConnection();

        this.voiceChannel = null;

        var radioJdaVoice = Radio.getInstance().getJda().getVoiceChannelById(voiceChannel);

        if (radioJdaVoice != null) {
            for (var listener : radioJdaVoice.getMembers()) {
                Service.of(ListeningTrackerManager.class).trackIfEligible(listener.getUser(), radioJdaVoice, listener.getVoiceState() == null || listener.getVoiceState().isDeafened());
            }
        }

        return true;
    }

    public String getVoiceChannel() {
        return voiceChannel;
    }

    @Override
    public void onShutdown() {
        if (jda != null) {
            jda.shutdown();

            jda = null;
        }
    }
}
