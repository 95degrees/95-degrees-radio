package me.voidinvoid;

import me.voidinvoid.tasks.TaskManager;
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

        System.out.println(ConsoleColor.BLUE_BACKGROUND + ConsoleColor.BLACK_BOLD + " Starting 95 Degrees Radio... " + ConsoleColor.RESET);

        if (!RadioConfig.loadFromFile(new File(args[0]))) {
            System.out.println(ConsoleColor.RED + "Failed to load config!" + ConsoleColor.RESET);
            return;
        }

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

    public void onEvent(Event e) {
        if (e instanceof ReadyEvent) {
            connectToGuild();
        }
    }

    public void connectToGuild() {
        VoiceChannel v = jda.getVoiceChannelById(config.channels.voice);
        AudioManager m = v.getGuild().getAudioManager();

        SongOrchestrator orch = new SongOrchestrator(this, jda, jda.getTextChannelById(config.channels.radioChat), jda.getTextChannelById(config.channels.djChat), config.channels.lyricsChat == null ? null : jda.getTextChannelById(config.channels.lyricsChat), v, config.locations.playlists);

        startTaskManager();

        m.setSendingHandler(orch.getHandler());
        //m.setReceivingHandler(orch.getKaraokeAudioListener());
        m.openAudioConnection(v);

        orch.playNextSong();
    }

    public void startTaskManager() {
        TaskManager.loadTasks(new File(config.locations.tasks));
    }

    public static void shutdown(boolean restart) {
        System.exit(restart ? 1 : 0);

        //SongOrchestrator.instance.getJda().shutdown();
        //SongOrchestrator.instance.getManager().shutdown();

        //TaskManager.shutdown();

        /*isRunning = false;

        if (restart) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    Thread.sleep(3000); //hack

                } catch (Exception ex) {
                    System.out.println("FAILED TO RESTART!!");
                    ex.printStackTrace();
                }
            }));
            try {
                Thread.sleep(1000);
            } catch (Exception ignored) {}

            System.exit(1);
        }*/
    }
}
