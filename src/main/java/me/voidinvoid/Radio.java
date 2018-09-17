package me.voidinvoid;

import me.voidinvoid.advertisements.AdvertisementManager;
import me.voidinvoid.coins.CoinCreditorManager;
import me.voidinvoid.commands.CommandManager;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.dj.SongDJ;
import me.voidinvoid.events.PlaylistTesterListener;
import me.voidinvoid.events.RadioMessageListener;
import me.voidinvoid.events.TotoAfricaSongListener;
import me.voidinvoid.karaoke.KaraokeManager;
import me.voidinvoid.status.StatusManager;
import me.voidinvoid.suggestions.SongSuggestionManager;
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

public class Radio implements EventListener {

    public static Radio instance;

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

        instance = new Radio(RadioConfig.config);
    }

    private RadioConfig config;
    private JDA jda;

    private SongOrchestrator orchestrator;

    private SongDJ dj;
    private KaraokeManager karaokeManager;
    private CommandManager commandManager;
    private StatusManager statusManager;
    private CoinCreditorManager coinCreditorManager;
    private SongSuggestionManager suggestionManager;
    private AdvertisementManager advertisementManager;

    public Radio(RadioConfig config) {
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

    private void startRadio() {

        jda.addEventListener(commandManager = new CommandManager());
        jda.addEventListener(suggestionManager = new SongSuggestionManager());
        if (RadioConfig.config.useCoinGain) jda.addEventListener(coinCreditorManager = new CoinCreditorManager(jda));

        orchestrator = new SongOrchestrator(this, config);

        PlaylistTesterListener tester = new PlaylistTesterListener(jda.getTextChannelById(config.channels.radioChat));
        jda.addEventListener(tester);

        orchestrator.registerSongEventListener(karaokeManager = new KaraokeManager());
        orchestrator.registerSongEventListener(dj = new SongDJ(orchestrator, jda.getTextChannelById(config.channels.djChat)));
        orchestrator.registerSongEventListener(new RadioMessageListener(jda.getTextChannelById(config.channels.radioChat)));
        orchestrator.registerSongEventListener(tester);
        if (!RadioConfig.config.debug) orchestrator.registerSongEventListener(new TotoAfricaSongListener(jda.getTextChannelById(config.channels.radioChat)));
        if (RadioConfig.config.useStatus) orchestrator.registerSongEventListener(statusManager = new StatusManager(jda));
        if (RadioConfig.config.useAdverts) orchestrator.registerSongEventListener(advertisementManager = new AdvertisementManager(jda));

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

    public StatusManager getStatusManager() {
        return statusManager;
    }

    public CoinCreditorManager getCoinCreditorManager() {
        return coinCreditorManager;
    }

    public void startTaskManager() {
        TaskManager.loadTasks(new File(config.locations.tasks));
    }

    public static void shutdown(boolean restart) {
        System.exit(restart ? 1 : 0);
    }

    public SongSuggestionManager getSuggestionManager() {
        return suggestionManager;
    }

    public AdvertisementManager getAdvertisementManager() {
        return advertisementManager;
    }
}