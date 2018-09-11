package me.voidinvoid.commands;

import me.voidinvoid.tasks.RadioTaskComposition;
import me.voidinvoid.tasks.TaskManager;
import me.voidinvoid.utils.ChannelScope;

public class TasksCommand extends Command {

    public TasksCommand() {
        super("tasks", "Lists all active tasks", null, ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        StringBuilder built = new StringBuilder("```[Tasks]\n\n");
        int i = 0;
        for (RadioTaskComposition c : TaskManager.getTasks()) {
            i++;

            built.append(i).append(": ").append(c.getName()).append(" (").append(c.getExecutionCron()).append(")\n");
        }

        built.append("```");
        data.getTextChannel().sendMessage(built.toString()).queue();
    }
}
