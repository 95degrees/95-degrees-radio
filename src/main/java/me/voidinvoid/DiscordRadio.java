package me.voidinvoid;

import me.voidinvoid.coins.CoinCreditorListener;
import me.voidinvoid.commands.CommandManager;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.dj.SongDJ;
import me.voidinvoid.karaoke.KaraokeManager;
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

    public static DiscordRadio instance;

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

        instance = new DiscordRadio(RadioConfig.config);
    }

    private RadioConfig config;
    private JDA jda;

    private SongOrchestrator orchestrator;

    private SongDJ dj;
    private KaraokeManager karaokeManager;
    private CommandManager commandManager;

    public DiscordRadio(RadioConfig config) {
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(config.botToken).addEventListener(this).build();

            if (config.useStatus) jda.getPresence().setGame(null);
        } catch (LoginException e) {
            System.out.println(ConsoleColor.RED + "Discord login failure!" + ConsoleColor.RESET);
            e.printStackTrace();
            return;
        }

        this.config = config;

        startTaskManager();
    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof ReadyEvent) {
            startRadio();
        }
    }

    public void startRadio() {

        commandManager = new CommandManager();

        orchestrator = new SongOrchestrator(this, config);

        orchestrator.registerSongEventListener(new SongDJ(orchestrator, jda.getTextChannelById(config.channels.djChat)));
        orchestrator.registerSongEventListener(new KaraokeManager());

        if (RadioConfig.config.useCoinGain) jda.addEventListener(new CoinCreditorListener(orchestrator));

        VoiceChannel radioVoiceChannel = jda.getVoiceChannelById(config.channels.voice);

        AudioManager mgr = radioVoiceChannel.getGuild().getAudioManager();

        mgr.setSendingHandler(orchestrator.getAudioSendHandler());
        mgr.openAudioConnection(radioVoiceChannel);

        orchestrator.playNextSong();
    }

    public JDA getJda() {
        return jda;
    }

    public SongOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public SongDJ getDj() {
        return dj;
    }

    public KaraokeManager getKaraokeManager() {
        return karaokeManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public void startTaskManager() {
        TaskManager.loadTasks(new File(config.locations.tasks));
    }

    public static void shutdown(boolean restart) {
        System.exit(restart ? 1 : 0);
    }
}