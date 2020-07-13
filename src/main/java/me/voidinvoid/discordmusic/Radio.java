package me.voidinvoid.discordmusic;

import me.voidinvoid.discordmusic.advertisements.AdvertisementManager;
import me.voidinvoid.discordmusic.cache.YouTubeCacheManager;
import me.voidinvoid.discordmusic.coins.CoinCreditorManager;
import me.voidinvoid.discordmusic.coins.RadioAwardsManager;
import me.voidinvoid.discordmusic.commands.CommandManager;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.economy.EconomyManager;
import me.voidinvoid.discordmusic.dj.SongDJ;
import me.voidinvoid.discordmusic.events.PlaylistTesterListener;
import me.voidinvoid.discordmusic.events.RadioMessageListener;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.events.SuggestionPrivateMessageManager;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.levelling.LevellingManager;
import me.voidinvoid.discordmusic.lyrics.LiveLyricsManager;
import me.voidinvoid.discordmusic.notifications.NotificationManager;
import me.voidinvoid.discordmusic.quiz.QuizManager;
import me.voidinvoid.discordmusic.ratings.SongRatingManager;
import me.voidinvoid.discordmusic.remotecontrol.RemoteSocketControlManager;
import me.voidinvoid.discordmusic.restream.RadioRestreamManager;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArtManager;
import me.voidinvoid.discordmusic.songs.database.SongTriggerManager;
import me.voidinvoid.discordmusic.spotify.SpotifyManager;
import me.voidinvoid.discordmusic.stats.UserStatisticsManager;
import me.voidinvoid.discordmusic.status.StatusManager;
import me.voidinvoid.discordmusic.suggestions.SongSuggestionManager;
import me.voidinvoid.discordmusic.tasks.TaskManager;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import me.voidinvoid.discordmusic.utils.Emoji;
import me.voidinvoid.discordmusic.utils.reactions.MessageReactionCallbackManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

public class Radio implements EventListener {

    private static Radio instance;
    public static String configName;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify the radio configuration name.");
            return;
        }

        configName = args[0];

        DatabaseManager db = new DatabaseManager(configName);

        if (!RadioConfig.load(db.getCollection("config").find(eq("_id", configName)).first())) {
            System.out.println(ConsoleColor.RED + "Failed to load config!" + ConsoleColor.RESET);
            return;
        }

        System.out.println(ConsoleColor.CYAN_BACKGROUND + ConsoleColor.BLACK_BOLD + " Starting 95 Degrees Radio... " + ConsoleColor.RESET);
        System.out.println("Loaded config: " + args[0]);

        instance = new Radio(RadioConfig.config, db);
    }

    private RadioConfig config;
    private JDA jda;
    private Guild guild;

    public static Radio getInstance() {
        return instance;
    }

    public Guild getGuild() {
        return guild;
    }

    public boolean reloadServices() {
        if (!RadioConfig.load(databaseManager.getCollection("config").find(eq("_id", configName)).first()))
            return false;

        radioServices.values().forEach(s -> {
            try {
                s.onShutdown();
                s.onLoad();
            } catch (Exception e) {
                System.out.println("Warning: error reloading service '" + s.getClass().getSimpleName() + "':");
                e.printStackTrace();
            }
        });

        return true;
    }

    private Map<Class, RadioService> radioServices = new HashMap<>();

    private SongOrchestrator orchestrator;
    private DatabaseManager databaseManager;

    private boolean loaded;

    public Radio(RadioConfig config, DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;

        try {
            jda = JDABuilder.createDefault(config.botToken).addEventListeners(this).build();

            if (config.useStatus) jda.getPresence().setActivity(null);
        } catch (LoginException e) {
            System.out.println(ConsoleColor.RED + "Discord login failure!" + ConsoleColor.RESET);
            e.printStackTrace();
            return;
        }

        this.config = config;
    }

    @Override
    public void onEvent(@Nonnull GenericEvent e) {
        if (!loaded && e instanceof ReadyEvent) {
            loaded = true;
            startRadio();
        }
    }

    private void startRadio() {
        TextChannel radioChannel = jda.getTextChannelById(config.channels.radioChat);
        TextChannel djChannel = jda.getTextChannelById(config.channels.djChat);

        guild = radioChannel.getGuild();

        VoiceChannel radioVoiceChannel = jda.getVoiceChannelById(config.channels.voice);

        EmbedBuilder loading = new EmbedBuilder().setTitle("⏱ Loading").setColor(Colors.ACCENT_LOADING).setDescription("`Loading playlists and songs...`").setThumbnail(jda.getSelfUser().getEffectiveAvatarUrl());
        Message msg = djChannel == null ? null : djChannel.sendMessage(loading.setTimestamp(OffsetDateTime.now()).build()).complete();

        registerService(databaseManager);
        registerService(new SpotifyManager());

        orchestrator = new SongOrchestrator(this, config);

        orchestrator.loadPlaylists();

        Emoji.init();

        if (djChannel != null)
            msg.editMessage(loading.appendDescription("\n`Loading song event hooks...`").setTimestamp(OffsetDateTime.now()).build()).queue();

        registerService(new AlbumArtManager());
        registerService(new MessageReactionCallbackManager());
        registerService(new SongRatingManager());
        registerService(new CommandManager());
        registerService(new SongSuggestionManager());
        registerService(new PlaylistTesterListener());
        registerService(new RPCSocketManager());
        registerService(new SongTriggerManager());
        registerService(new NotificationManager());
        //registerService(new TickerManager());
        registerService(new SongDJ());
        registerService(new RadioMessageListener());
        registerService(new QuizManager());
        registerService(new CoinCreditorManager());
        registerService(new StatusManager());
        registerService(new AdvertisementManager());
        registerService(new EconomyManager());
        registerService(new LevellingManager());
        registerService(new AchievementManager());
        registerService(new TaskManager());
        registerService(new UserStatisticsManager());
        registerService(new SuggestionPrivateMessageManager());
        registerService(new RemoteSocketControlManager());
        //registerService(new RadioPauseManager());
        registerService(new YouTubeCacheManager());
        registerService(new RadioRestreamManager());
        registerService(new RadioAwardsManager());
        registerService(new LiveLyricsManager());

        if (djChannel != null)
            msg.editMessage(loading.appendDescription("\n`Opening audio connection...`").setTimestamp(OffsetDateTime.now()).build()).queue();

        AudioManager mgr = radioVoiceChannel.getGuild().getAudioManager();

        mgr.setSendingHandler(orchestrator.getAudioSendHandler());
        mgr.openAudioConnection(radioVoiceChannel);

        if (djChannel != null)
            msg.editMessage(loading.appendDescription("\n`Load complete!`").setTimestamp(OffsetDateTime.now()).build()).queue();

        if (djChannel != null) msg.delete().queueAfter(8, TimeUnit.SECONDS);

        orchestrator.playNextSong();
    }

    private void registerService(RadioService service) {
        if (getServices().stream().anyMatch(r -> r.getClass() == service.getClass())) {
            System.out.println("Skipping service '" + service.getClass().getSimpleName() + "' as an instance of it is already running");
            return;
        }

        if (!service.canRun(config)) {
            System.out.println("Skipping service '" + service.getClass().getSimpleName() + "' since it can't run with the current config");
            return;
        }

        radioServices.put(service.getClass(), service);

        if (service instanceof SongEventListener)
            orchestrator.registerSongEventListener(((SongEventListener) service));
        if (service instanceof EventListener) jda.addEventListener(service);

        try {
            service.onLoad();
        } catch (Exception e) {
            System.out.println("Warning: Error loading service '" + service.getClass().getSimpleName() + "':");
            e.printStackTrace();
        }
    }

    public <T> T getService(Class<T> service) {
        Object srv = radioServices.get(service);

        if (srv == null) {
            try {
                srv = service.getDeclaredConstructor().newInstance();
                System.out.println("Dynamically created '" + service.getSimpleName() + "' instance since it did not exist already");

                registerService((RadioService) srv);
            } catch (Exception ex) {
                System.err.println("Error dynamically creating new instance of '" + service.getSimpleName() + "'");
            }
        }

        return service.cast(srv);
    }

    public JDA getJda() {
        return jda;
    }

    public SongOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public void shutdown(boolean restart) {
        System.out.println("Deactivating playlist...");
        if (orchestrator.getActivePlaylist() != null) orchestrator.getActivePlaylist().onDeactivate();
        var sv = getServices();
        var size = sv.size();

        int i = 0;
        for (RadioService radioService : sv) {
            try {
                i++;
                System.out.println("(" + i + "/" + size + ") Shutting down service '" + radioService.getClass().getSimpleName() + "'...");
                radioService.onShutdown();
            } catch (Exception e) {
                System.out.println("Warning: Error shutting down service '" + radioService.getClass().getSimpleName() + "':");
                e.printStackTrace();
            }
        }
        System.exit(restart ? 1 : 0);
    }

    public Collection<RadioService> getServices() {
        return radioServices.values();
    }
}