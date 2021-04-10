package me.voidinvoid.discordmusic.commands.slash;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.commands.slash.impl.*;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Emoji;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Guardian - 16/12/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

public class SlashCommandManager extends ListenerAdapter implements RadioService {

    private Map<CommandUpdateAction.CommandData, SlashCommandHandler> commands = new HashMap<>();
    private Map<List<String>, Method> handlers = new HashMap<>();

    @Override
    public void onLoad() {
        registerCommandHandler(new PlaySongCommand());
        registerCommandHandler(new QueueCommand());
        registerCommandHandler(new SkipSongCommand());
        registerCommandHandler(new SpotifyPresenceCopyCommand());
        registerCommandHandler(new RestreamCommand());

        registerCommands();
    }

    public void registerCommandHandler(SlashCommandHandler handler) {
        var cmd = handler.getCommand();
        var name = cmd.toData().getString("name");

        commands.put(cmd, handler);

        for (var method : handler.getClass().getDeclaredMethods()) {
            var handlerAnnotation = method.getAnnotation(CommandHandler.class);

            if (handlerAnnotation == null) {
                continue;
            }

            var path = new ArrayList<String>();

            path.add(name);
            path.addAll(Arrays.asList(handlerAnnotation.value()));

            this.handlers.put(path, method);
            log("Registered handler " + String.join(", ", path));
        }
    }

    public Map<CommandUpdateAction.CommandData, SlashCommandHandler> getCommands() {
        return commands;
    }

    public void registerCommands() {
        var guild = Radio.getInstance().getGuild();
        guild.updateCommands().addCommands(commands.keySet()).queue();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent e) {
        var name = e.getName();

        var command = commands.entrySet().stream().filter(c -> c.getKey().getName().equals(name)).map(Map.Entry::getValue).findFirst().orElse(null);

        if (command == null) {
            log("No command handler for " + name + " command");
            return;
        }

        var handlerMethod = findAppropriateHandler(e);

        if (handlerMethod == null) {
            log("No subcommand handler for " + name + " command");
            return;
        }

        var data = new SlashCommandData(e, command.getCommand());

        if (data.getEvent().getMember() == null) {
            log("Unknown member");
            return;
        }

        if (command.requiresDjAccess() && !ChannelScope.DJ_CHAT.hasAccess(data.getEvent().getMember())) {
            data.sendMessage(Emoji.WARN + "**Permission Error**\n> You don't have permission to use this command", true);
            return;
        }

        log("Invoking command handler for /" + name);
        try {
            try {
                handlerMethod.invoke(command, data);
            } catch (InvocationTargetException ex) {
                data.sendMessage(Emoji.WARN + " • **An error occurred executing this slash command** - please notify a staff member\n> Command: /" + command.getCommand().getName() + "\n> Exception: " + ex.getTargetException().getMessage() + (ex.getTargetException().getStackTrace().length > 0 ? ", " + ex.getTargetException().getStackTrace()[0].getClassName() + ":" + ex.getTargetException().getStackTrace()[0].getLineNumber() : ""), true);
                ex.getTargetException().printStackTrace();
            }

        } catch (Exception ex) {
            log("Error executing /" + name);
            ex.printStackTrace();
        }
    }

    public Method findAppropriateHandler(SlashCommandEvent event) {
        var path = event.getCommandPath().split("/");

        return handlers.get(List.of(path));
    }
}
