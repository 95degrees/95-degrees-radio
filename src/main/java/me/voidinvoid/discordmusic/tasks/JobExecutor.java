package me.voidinvoid.discordmusic.tasks;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class JobExecutor implements Job {

    private static final String TASK_LOG_PREFIX = ConsoleColor.PURPLE_BACKGROUND + " TASK EXECUTOR " + ConsoleColor.RESET_SPACE;

    @Override
    public void execute(JobExecutionContext ctx) throws JobExecutionException {
        try {
            RadioTaskComposition comp = (RadioTaskComposition) ctx.getJobDetail().getJobDataMap().get("comp");
            executeComposition(comp, false);
        } catch (Exception e) {
            System.out.println(TASK_LOG_PREFIX + "Scheduler task exception");
            e.printStackTrace();
        }
    }

    public void executeComposition(RadioTaskComposition comp, boolean ignoreCancellation) {
        try {
            if (comp.isCancelled() && !ignoreCancellation) {
                System.out.println(TASK_LOG_PREFIX + "Ignoring task invocation due to being cancelled");
                comp.setCancelled(false);
                return;
            }
            System.out.println(TASK_LOG_PREFIX + "Invoking task " + (comp.getName() == null ? "<unnamed>" : comp.getName()));
            comp.getTasks().forEach(r -> r.invoke(Radio.getInstance().getOrchestrator()));
        } catch (Exception e) {
            System.out.println(TASK_LOG_PREFIX + "Error invoking task");
            e.printStackTrace();
        }
    }
}
