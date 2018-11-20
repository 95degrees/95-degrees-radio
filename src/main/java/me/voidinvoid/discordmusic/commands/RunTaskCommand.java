package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.tasks.RadioTaskComposition;
import me.voidinvoid.discordmusic.tasks.TaskManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;

public class RunTaskCommand extends Command {

    RunTaskCommand() {
        super("run-task", "Manually runs a task", "<task#>", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        if (args.length < 1) {
            data.error("Task ID required. Use `!tasks` to list tasks");
            return;
        }

        int id;
        try {
            id = Integer.valueOf(args[0]);
        } catch (Exception ignored) {
            data.error("Invalid task ID. Use `!tasks` to list tasks");
            return;
        }

        if (id < 1 || id > TaskManager.getTasks().size()) {
            data.error("Invalid task ID. Use `!tasks` to list tasks");
            return;
        }

        RadioTaskComposition comp = TaskManager.getTasks().get(id - 1);
        TaskManager.executeComposition(comp, true);

        data.success("Executed task");
    }
}