package me.voidinvoid.discordmusic.restream;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.audio.AudioPlayerSendHandler;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
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
            log("Warning: tried to join vc but not connected");
            return;
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
    }

    public void leaveVoice(String guild) { //use string because we want a guild for our jda instance

        var g = jda.getGuildById(guild);

        if (g == null) {
            log("Warning: attempted to find null guild " + guild);
            return;
        }

        g.getAudioManager().closeAudioConnection();
    }

    @Override
    public void onShutdown() {
        if (jda != null) {
            jda.shutdown();

            jda = null;
        }
    }
}
