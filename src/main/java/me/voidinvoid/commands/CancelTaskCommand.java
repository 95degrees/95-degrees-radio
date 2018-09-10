package me.voidinvoid.commands;

import me.voidinvoid.tasks.RadioTaskComposition;
import me.voidinvoid.tasks.TaskManager;

public class CancelTaskCommand extends Command {

    public CancelTaskCommand() {
        super("cancel-task", "Cancels the next execution of a task", "<task#>", CommandScope.DJ_CHAT);
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
        comp.setCancelled(true);

        data.success("Cancelled task");
    }
}
