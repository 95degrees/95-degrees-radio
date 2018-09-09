package me.voidinvoid;

import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.dj.SongDJ;
import me.voidinvoid.tasks.TaskManager;
import me.voidinvoid.utils.ConsoleColor;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.managers.AudioManager;

import javax.security.auth.login.LoginException;
import java.io.File;

public class DiscordRadio implements EventListener {

    public static boolean isRunning = true;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify the radio settings file location as an argument.");
            return;
        }

        if (!RadioConfig.loadFromFile(new File(args[0]))) {
            System.out.println(ConsoleColor.RED + "Failed to load config!" + ConsoleColor.RESET);
            return;
        }

        System.out.println(ConsoleColor.BLUE_BACKGROUND + ConsoleColor.BLACK_BOLD + " Starting 95 Degrees Radio... " + ConsoleColor.RESET);

        new DiscordRadio(RadioConfig.config);
    }

    private RadioConfig config;
    private JDA jda;

    public DiscordRadio(RadioConfig config) {
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(config.botToken).addEventListener(this).build();

            if (config.useStatus) jda.getPresence().setGame(null);
        } catch (LoginException e) {
            e.printStackTrace();
            return;
        }

        this.config = config;
    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof ReadyEvent) {
            connectToGuild();
        }
    }

    public void connectToGuild() {
        VoiceChannel radioVoiceChannel = jda.getVoiceChannelById(config.channels.voice);

        SongOrchestrator orch = new SongOrchestrator(this, jda, jda.getTextChannelById(config.channels.radioChat), config.channels.lyricsChat == null ? null : jda.getTextChannelById(config.channels.lyricsChat), radioVoiceChannel, config.locations.playlists);

        orch.registerSongEventListener(new SongDJ(orch, jda.getTextChannelById(config.channels.djChat)));
        orch.registerSongEventListener(new SongDJ(orch, jda.getTextChannelById(config.channels.lyricsChat))); //todo TEMP

        startTaskManager();

        AudioManager m = radioVoiceChannel.getGuild().getAudioManager();

        m.setSendingHandler(orch.getHandler());
        m.openAudioConnection(radioVoiceChannel);

        orch.playNextSong();
    }

    public void startTaskManager() {
        TaskManager.loadTasks(new File(config.locations.tasks));
    }

    public static void shutdown(boolean restart) {
        System.exit(restart ? 1 : 0);
    }
}
