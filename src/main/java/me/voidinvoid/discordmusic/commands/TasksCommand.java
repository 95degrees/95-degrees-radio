package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.tasks.RadioTaskComposition;
import me.voidinvoid.discordmusic.tasks.TaskManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Rank;

public class TasksCommand extends Command {

    TasksCommand() {
        super("tasks", "Lists all active tasks", null, Rank.DJ);
    }

    @Override
    public void invoke(CommandData data) {
        StringBuilder built = new StringBuilder("[Tasks]\n\n");
        int i = 0;
        for (RadioTaskComposition c : Radio.getInstance().getService(TaskManager.class).getTasks()) {
            i++;

            built.append(i).append(": ").append(c.getName()).append(" (").append(c.getExecutionCron()).append(")\n");
        }

        data.code(built.toString());
    }
}
