package me.voidinvoid.commands;

import me.voidinvoid.config.RadioConfig;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class CommandManager implements EventListener {

    private final List<Command> commands = new ArrayList<>();

    public CommandManager() {
        register(new QueueCommand());
        register(new YouTubeSearchCommand());
        register(new PlaylistsCommand());
        register(new PlaySongCommand());
        register(new SeekCommand());
        register(new SkipCommand());
        register(new ShuffleCommand());
        register(new SongsCommand());
        register(new FindSongCommand());
        register(new QueueSongCommand());
        register(new SwitchPlaylistCommand());
        register(new AnnounceCommand());
        register(new KaraokeCommand());
        register(new TasksCommand());
        register(new AdvertisementCommand());
        register(new RunTaskCommand());
        register(new CancelTaskCommand());
        register(new ReloadCommand());
        register(new RestartRadioCommand());
        register(new StopRadioCommand());
        register(new HelpCommand(commands));

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
                        System.out.println("Unknown command. Use 'radio-commands' to list commands");
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
    public void onEvent(Event ev) {
        if (ev instanceof GuildMessageReceivedEvent) {
            GuildMessageReceivedEvent e = ((GuildMessageReceivedEvent) ev);

            String id = e.getChannel().getId();
            String msg = e.getMessage().getContentRaw();

            if (!id.equals(RadioConfig.config.channels.djChat) && !id.equals(RadioConfig.config.channels.radioChat))
                return;
            if (!msg.startsWith(Command.COMMAND_PREFIX) || msg.length() == Command.COMMAND_PREFIX.length()) return;

            String fullCmd = msg.contains(" ") ? msg.substring(0, msg.indexOf(" ")) : msg;
            String cmd = fullCmd.substring(Command.COMMAND_PREFIX.length()); //get rid of prefix

            commands.stream()
                    .filter(c -> c.getName().equalsIgnoreCase(cmd) || Arrays.stream(c.getAliases()).anyMatch(a -> a.equalsIgnoreCase(cmd))) //check cmd name and aliases
                    .filter(c -> c.getScope().check(e.getChannel())) //check if allowed to run in this channel
                    .findFirst()
                    .ifPresent(c -> c.invoke(new CommandData(e.getMember(), e.getChannel(), e.getMessage(), c, cmd))); //invokes cmd if matched
        }
    }
}