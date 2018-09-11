package me.voidinvoid.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.voidinvoid.Radio;
import me.voidinvoid.utils.ConsoleColor;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.quartz.CronScheduleBuilder.cronSchedule;

public class TaskManager implements Job {

    private static final String TASK_LOG_PREFIX = ConsoleColor.PURPLE_BACKGROUND + " TASK " + ConsoleColor.RESET_SPACE;

    private static Scheduler scheduler;

    private static List<RadioTaskComposition> tasks;

    public static void loadTasks(File file) {
        try {
            tasks = new ArrayList<>();

            if (scheduler != null) {
                scheduler.clear();
                scheduler.shutdown(false);
            }

            Gson gson = new Gson();
            JsonArray arr = new JsonParser().parse(new String(Files.readAllBytes(file.toPath()))).getAsJsonObject().get("task_compositions").getAsJsonArray();

            for (JsonElement e : arr) {
                System.out.println(TASK_LOG_PREFIX + "Found task composition");
                tasks.add(new RadioTaskComposition(gson, e.getAsJsonObject()));
            }

            scheduler = StdSchedulerFactory.getDefaultScheduler();

            int i = 0;
            for (RadioTaskComposition t : tasks) {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("comp", t);

                    System.out.println(TASK_LOG_PREFIX + "Scheduling task for " + t.getExecutionCron());

                    scheduler.scheduleJob(
                            JobBuilder.newJob(TaskManager.class)
                                    .withIdentity("job_" + i++)
                                    .usingJobData(new JobDataMap(data)).build(),

                            TriggerBuilder.newTrigger().withIdentity("trigger_" + i).withSchedule(cronSchedule(t.getExecutionCron())).build());

                    scheduler.start();
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void execute(JobExecutionContext ctx) {
        try {
            RadioTaskComposition comp = (RadioTaskComposition) ctx.getJobDetail().getJobDataMap().get("comp");
            executeComposition(comp, false);
        } catch (Exception e) {
            System.out.println(TASK_LOG_PREFIX + "Scheduler task exception");
            e.printStackTrace();
        }
    }

    public static void executeComposition(RadioTaskComposition comp, boolean ignoreCancellation) {
        try {
            if (comp.isCancelled() && !ignoreCancellation) {
                System.out.println(TASK_LOG_PREFIX + "Ignoring task invocation due to being cancelled");
                comp.setCancelled(false);
                return;
            }
            System.out.println(TASK_LOG_PREFIX + "Invoking task " + (comp.getName() == null ? "<unnamed>" : comp.getName()));
            comp.getTasks().forEach(r -> r.invoke(Radio.instance.getOrchestrator()));
        } catch (Exception e) {
            System.out.println(TASK_LOG_PREFIX + "Error invoking task");
            e.printStackTrace();
        }
    }

    public static List<RadioTaskComposition> getTasks() {
        return tasks;
    }

    public static void shutdown() {
        try {
            scheduler.shutdown(false);
        } catch (SchedulerException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
