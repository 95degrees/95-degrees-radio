package me.voidinvoid.discordmusic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import org.bson.Document;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.quartz.CronScheduleBuilder.cronSchedule;

public class TaskManager implements RadioService {

    private static final String TASK_LOG_PREFIX = ConsoleColor.PURPLE_BACKGROUND + " TASK " + ConsoleColor.RESET_SPACE;

    private static Scheduler scheduler;

    private List<RadioTaskComposition> tasks;

    @Override
    public void onLoad() {
        try {
            tasks = new ArrayList<>();

            if (scheduler != null) {
                scheduler.clear();
                scheduler.shutdown(false);
            }

            DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);
            if (db != null) {
                db.getCollection("tasks").find().forEach((Consumer<? super Document>) comp -> {
                    log(TASK_LOG_PREFIX + "Found database task composition");
                    tasks.add(new RadioTaskComposition(comp));
                });
            }

            if (RadioConfig.config.locations.tasks != null) {
                Gson gson = new Gson();
                JsonArray arr = new JsonParser().parse(new String(Files.readAllBytes(Paths.get(RadioConfig.config.locations.tasks)))).getAsJsonObject().get("task_compositions").getAsJsonArray();

                for (JsonElement e : arr) {
                    log(TASK_LOG_PREFIX + "Found task composition");
                    tasks.add(new RadioTaskComposition(gson, e.getAsJsonObject()));
                }
            }

            scheduler = StdSchedulerFactory.getDefaultScheduler();

            int i = 0;
            for (RadioTaskComposition t : tasks) {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("comp", t);

                    log(TASK_LOG_PREFIX + "Scheduling task for " + t.getExecutionCron());

                    scheduler.scheduleJob(
                            JobBuilder.newJob(JobExecutor.class)
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
        }
    }

    public List<RadioTaskComposition> getTasks() {
        return tasks;
    }

    public void executeComposition(RadioTaskComposition comp, boolean ignoreCancellation) {
        TextChannel djChannel = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.djChat);

        try {
            if (comp.isCancelled() && !ignoreCancellation) {
                log(TASK_LOG_PREFIX + "Ignoring task invocation due to being cancelled");
                comp.setCancelled(false);
                return;
            }
            log(TASK_LOG_PREFIX + "Invoking task " + (comp.getName() == null ? "<unnamed>" : comp.getName()));
            comp.getTasks().forEach(r -> r.invoke(Radio.getInstance().getOrchestrator()));

            djChannel.sendMessage(new EmbedBuilder().setTitle("➡ Executed task " + comp.getName()).setColor(Colors.ACCENT_TASK_SUCCESS).build()).queue(m -> m.delete().queueAfter(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            log(TASK_LOG_PREFIX + "Error invoking task");
            e.printStackTrace();
            djChannel.sendMessage(new EmbedBuilder().setTitle("⚠ Error executing task " + comp.getName()).setColor(Colors.ACCENT_TASK_ERROR).build()).queue();
        }
    }

    public void shutdown() {
        try {
            scheduler.shutdown(false);
        } catch (SchedulerException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
