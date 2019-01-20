package me.voidinvoid.discordmusic.tasks;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class JobExecutor implements Job {

    private static final String TASK_LOG_PREFIX = ConsoleColor.PURPLE_BACKGROUND + " TASK EXECUTOR " + ConsoleColor.RESET_SPACE;

    @Override
    public void execute(JobExecutionContext ctx) {
        try {
            TaskManager taskManager = Radio.getInstance().getService(TaskManager.class);
            RadioTaskComposition comp = (RadioTaskComposition) ctx.getJobDetail().getJobDataMap().get("comp");
            taskManager.executeComposition(comp, false);
        } catch (Exception e) {
            System.out.println(TASK_LOG_PREFIX + "Scheduler task exception");
            e.printStackTrace();
        }
    }
}
