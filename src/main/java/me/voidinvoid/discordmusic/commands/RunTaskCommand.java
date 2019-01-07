package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
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

        TaskManager taskManager = Radio.getInstance().getService(TaskManager.class);

        if (id < 1 || id > taskManager.getTasks().size()) {
            data.error("Invalid task ID. Use `!tasks` to list tasks");
            return;
        }

        RadioTaskComposition comp = taskManager.getTasks().get(id - 1);
        taskManager.executeComposition(comp, true);

        data.success("Executed task");
    }
}
