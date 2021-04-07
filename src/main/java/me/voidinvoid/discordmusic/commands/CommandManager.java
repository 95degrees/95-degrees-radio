package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class CommandManager implements RadioService, EventListener {

    private final List<Command> commands = new ArrayList<>();

    public CommandManager() {
        register(new QueueCommand());
        register(new PlaylistsCommand());
        register(new PlaySongCommand());
        register(new SeekCommand());
        register(new SkipCommand());
        register(new ShuffleCommand());
        register(new LevelCommand());
        register(new SongsCommand());
        register(new FindSongCommand());
        register(new QueueSongCommand());
        register(new AddSongCommand());
        register(new SwitchPlaylistCommand());
        register(new AnnounceCommand());
        //register(new KaraokeCommand());
        register(new TasksCommand());
        register(new SetSpotifyMappingCommand());
        register(new AdvertisementCommand());
        register(new PollCommand());
        register(new SongRatingsCommand());
        register(new RunTaskCommand());
        register(new CancelTaskCommand());
        register(new SetStatusCommand());
        register(new ReloadCommand());
        register(new RestartRadioCommand());
        register(new StopRadioCommand());
        register(new DebugCommand());
        register(new ServicesCommand());
        if (RadioConfig.config.useQuizSocketServer) register(new QuizSocketServerKeyCommand());
        register(new CommandListCommand(commands));

        Executors.newSingleThreadExecutor().submit(() -> {
            Scanner s = new Scanner(System.in);
            while (s.hasNextLine()) {
                try {
                    String msg = s.nextLine();

                    String cmdName = msg.contains(" ") ? msg.substring(0, msg.indexOf(" ")) : msg;

                    Command match = commands.stream()
                            .filter(c -> c.getName().equalsIgnoreCase(cmdName) || Arrays.stream(c.getAliases()).anyMatch(a -> a.equalsIgnoreCase(cmdName))) //check cmd name and aliases
                            .findFirst()
                            .orElse(null);

                    if (match == null) {
                        log("Unknown command. Use 'c' to list commands");
                    } else if (!match.isAllowConsole()) {
                        log("This command doesn't support being ran from the console");
                    } else {
                        match.invoke(new CommandData(msg, match, cmdName));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void register(Command cmd) {
        commands.add(cmd);
    }

    @Override
    public void onEvent(@Nonnull GenericEvent ev) {
        if (ev instanceof GuildMessageReceivedEvent) {
            GuildMessageReceivedEvent e = ((GuildMessageReceivedEvent) ev);

            String id = e.getChannel().getId();
            String msg = e.getMessage().getContentRaw();

            if (!msg.startsWith(Command.COMMAND_PREFIX) || msg.length() == Command.COMMAND_PREFIX.length()) return;

            String fullCmd = msg.contains(" ") ? msg.substring(0, msg.indexOf(" ")) : msg;
            String cmd = fullCmd.substring(Command.COMMAND_PREFIX.length()); //get rid of prefix

            commands.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(cmd) || Arrays.stream(c.getAliases()).anyMatch(a -> a.equalsIgnoreCase(cmd))) //check cmd name and aliases
                    .filter(c -> c.getRank().hasRank(e.getMember())) //check if user has permission
                    .findFirst()
                    .ifPresent(c -> c.invoke(new CommandData(e.getMember(), e.getChannel(), e.getMessage(), c, cmd))); //invokes cmd if matched
        }
    }
}