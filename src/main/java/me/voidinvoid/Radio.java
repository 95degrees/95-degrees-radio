package me.voidinvoid;

import me.voidinvoid.advertisements.AdvertisementManager;
import me.voidinvoid.coins.CoinCreditorManager;
import me.voidinvoid.commands.CommandManager;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.dj.SongDJ;
import me.voidinvoid.events.PlaylistTesterListener;
import me.voidinvoid.events.RadioMessageListener;
import me.voidinvoid.events.SongEventListener;
import me.voidinvoid.events.TotoAfricaSongListener;
import me.voidinvoid.karaoke.KaraokeManager;
import me.voidinvoid.server.SocketServer;
import me.voidinvoid.status.StatusManager;
import me.voidinvoid.suggestions.SongSuggestionManager;
import me.voidinvoid.tasks.TaskManager;
import me.voidinvoid.utils.ConsoleColor;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.managers.AudioManager;
import net.dv8tion.jda.core.requests.RequestFuture;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.time.OffsetDateTime;

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
    private SocketServer socketServer;

    private boolean loaded;

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
        if (!loaded && e instanceof ReadyEvent) {
            loaded = true;
            startRadio();
        }
    }

    private void startRadio() {
        TextChannel radioChannel = jda.getTextChannelById(config.channels.radioChat);
        TextChannel djChannel = jda.getTextChannelById(config.channels.djChat);

        VoiceChannel radioVoiceChannel = jda.getVoiceChannelById(config.channels.voice);

        Message msg = djChannel.sendMessage(new EmbedBuilder().setTitle("Loading").setDescription("95 Degrees Radio is starting up...\n`Loading playlists and songs...`").setThumbnail(jda.getSelfUser().getEffectiveAvatarUrl()).setTimestamp(OffsetDateTime.now()).build()).complete();

        orchestrator = new SongOrchestrator(this, config);

        msg.editMessage(new EmbedBuilder().setTitle("Loading").setDescription("95 Degrees Radio is starting up...\n`Loading playlists and songs...`").setThumbnail(jda.getSelfUser().getEffectiveAvatarUrl()).setTimestamp(OffsetDateTime.now()).build()).queue();

        register(commandManager = new CommandManager());
        register(suggestionManager = new SongSuggestionManager());
        register(new PlaylistTesterListener(radioChannel));
        register(socketServer = new SocketServer(radioVoiceChannel));

        register(karaokeManager = new KaraokeManager());
        register(dj = new SongDJ(orchestrator, djChannel));
        register(new RadioMessageListener(radioChannel));

        if (RadioConfig.config.useCoinGain) register(coinCreditorManager = new CoinCreditorManager(jda));
        if (!RadioConfig.config.debug) register(new TotoAfricaSongListener(radioChannel));
        if (RadioConfig.config.useStatus) register(statusManager = new StatusManager(jda));
        if (RadioConfig.config.useAdverts) register(advertisementManager = new AdvertisementManager(jda));

        AudioManager mgr = radioVoiceChannel.getGuild().getAudioManager();

        mgr.setSendingHandler(orchestrator.getAudioSendHandler());
        mgr.openAudioConnection(radioVoiceChannel);

        orchestrator.playNextSong();
    }

    private void register(Object listener) {
        if (listener instanceof SongEventListener) orchestrator.registerSongEventListener(((SongEventListener) listener));
        if (listener instanceof EventListener) jda.addEventListener(listener);
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

    public SocketServer getSocketServer() {
        return socketServer;
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