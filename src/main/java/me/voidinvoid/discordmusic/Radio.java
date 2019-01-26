package me.voidinvoid.discordmusic;

import me.voidinvoid.discordmusic.advertisements.AdvertisementManager;
import me.voidinvoid.discordmusic.coins.CoinCreditorManager;
import me.voidinvoid.discordmusic.commands.CommandManager;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.dj.SongDJ;
import me.voidinvoid.discordmusic.events.LaMetricMemberStatsHook;
import me.voidinvoid.discordmusic.events.PlaylistTesterListener;
import me.voidinvoid.discordmusic.events.RadioMessageListener;
import me.voidinvoid.discordmusic.events.SongEventListener;
import me.voidinvoid.discordmusic.karaoke.KaraokeManager;
import me.voidinvoid.discordmusic.levelling.AchievementManager;
import me.voidinvoid.discordmusic.levelling.LevellingManager;
import me.voidinvoid.discordmusic.quiz.QuizManager;
import me.voidinvoid.discordmusic.ratings.SongRatingManager;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.songs.database.SongTriggerManager;
import me.voidinvoid.discordmusic.status.StatusManager;
import me.voidinvoid.discordmusic.suggestions.SongSuggestionManager;
import me.voidinvoid.discordmusic.tasks.TaskManager;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import me.voidinvoid.discordmusic.utils.reactions.MessageReactionCallbackManager;
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

import javax.security.auth.login.LoginException;
import java.io.File;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;

public class Radio implements EventListener {

    private static Radio instance;
    private static String configName;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify the radio configuration name.");
            return;
        }

        DatabaseManager db = new DatabaseManager();

        configName = args[0];

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

    public static Radio getInstance() {
        return instance;
    }

    private static void reloadConfig() {
        //TODO
    }

    private Map<Class, Object> radioServices = new HashMap<>();

    private SongOrchestrator orchestrator;
    private DatabaseManager databaseManager;

    private boolean loaded;

    public Radio(RadioConfig config, DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;

        try {
            jda = new JDABuilder(AccountType.BOT).setToken(config.botToken).addEventListener(this).build();

            if (config.useStatus) jda.getPresence().setGame(null);
        } catch (LoginException e) {
            System.out.println(ConsoleColor.RED + "Discord login failure!" + ConsoleColor.RESET);
            e.printStackTrace();
            return;
        }

        this.config = config;
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

        EmbedBuilder loading = new EmbedBuilder().setTitle("⏱ Loading").setColor(Colors.ACCENT_LOADING).setDescription("`Loading playlists and songs...`").setThumbnail(jda.getSelfUser().getEffectiveAvatarUrl());
        Message msg = djChannel == null ? null : djChannel.sendMessage(loading.setTimestamp(OffsetDateTime.now()).build()).complete();

        registerService(databaseManager);
        orchestrator = new SongOrchestrator(this, config);

        if (djChannel != null)
            msg.editMessage(loading.appendDescription("\n`Loading song event hooks...`").setTimestamp(OffsetDateTime.now()).build()).queue();

        registerService(new MessageReactionCallbackManager());
        registerService(new SongRatingManager());
        registerService(new CommandManager());
        registerService(new SongSuggestionManager());
        registerService(new PlaylistTesterListener(radioChannel));
        if (RadioConfig.config.useSocketServer) registerService(new RPCSocketManager(radioVoiceChannel));
        registerService(new SongTriggerManager());
        registerService(new KaraokeManager());
        registerService(new SongDJ());
        registerService(new RadioMessageListener(radioChannel));
        registerService(new QuizManager(Paths.get(RadioConfig.config.locations.quizzes), radioChannel, djChannel, radioChannel.getGuild().getRoleById(RadioConfig.config.roles.quizInGameRole), radioChannel.getGuild().getRoleById(RadioConfig.config.roles.quizEliminatedRole)));
        if (RadioConfig.config.useCoinGain) registerService(new CoinCreditorManager(jda, orchestrator.getActivePlaylist()));
        if (RadioConfig.config.useStatus) registerService(new StatusManager(jda));
        if (RadioConfig.config.useAdverts) registerService(new AdvertisementManager(jda));
        if (RadioConfig.config.useSocketServer && !RadioConfig.config.debug) registerService(new LaMetricMemberStatsHook()); //todo
        registerService(new LevellingManager());
        registerService(new AchievementManager());
        registerService(new TaskManager());

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

    private void registerService(Object service) {
        radioServices.put(service.getClass(), service);

        if (service instanceof SongEventListener)
            orchestrator.registerSongEventListener(((SongEventListener) service));
        if (service instanceof EventListener) jda.addEventListener(service);
    }

    public <T> T getService(Class<T> service) {
        Object srv = radioServices.get(service);
        if (srv == null) return null;

        return service.cast(srv);
    }

    public JDA getJda() {
        return jda;
    }

    public SongOrchestrator getOrchestrator() {
        return orchestrator;
    }

    public void shutdown(boolean restart) {
        if (orchestrator.getActivePlaylist() != null) orchestrator.getActivePlaylist().onDeactivate();
        System.exit(restart ? 1 : 0);
    }
}