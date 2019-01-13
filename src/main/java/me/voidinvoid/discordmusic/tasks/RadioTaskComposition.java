package me.voidinvoid.discordmusic.tasks;

import com.google.gson.*;
import me.voidinvoid.discordmusic.tasks.types.TaskType;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadioTaskComposition {

    private String name;
    private List<RadioTask> tasks = new ArrayList<>();
    private String executionCron;

    private boolean cancelled;

    @SuppressWarnings("unchecked")
    public RadioTaskComposition(Document elem) {
        if (!elem.containsKey("tasks")) {
            throw new JsonSyntaxException("No 'tasks' specified for radio task");
        }

        if (!elem.containsKey("execution_time")) {
            throw new JsonSyntaxException("No 'execution_time' specified for radio task");
        }

        if (elem.containsKey("label")) {
            name = elem.getString("label");
        }

        executionCron = elem.getString("execution_time");

        List<Document> rawTasks = (List<Document>) elem.get("tasks");

        for (Document obj : rawTasks) {
            TaskType type = TaskType.valueOf(obj.getString("type"));
            Document rawParams = obj.containsKey("params") ? obj.get("params", Document.class) : null;

            Map<Parameter, Object> paramMap = new HashMap<>();

            for (Parameter p : type.getParams()) {
                if (!p.isOptional() && (rawParams == null || !rawParams.containsKey(p.getName()))) {
                    throw new JsonSyntaxException("Mandatory parameter '" + p.getName() + "' has no value");
                }
                paramMap.put(p, rawParams != null && rawParams.containsKey(p.getName()) ? rawParams.get(p.getName()) : null);
            }

            tasks.add(new RadioTask(type.getExecutor(), new ParameterList(paramMap)));
        }
    }

    public RadioTaskComposition(Gson gson, JsonObject elem) {
        if (!elem.has("tasks")) {
            throw new JsonSyntaxException("No 'tasks' specified for radio task");
        }

        if (!elem.has("execution_time")) {
            throw new JsonSyntaxException("No 'execution_time' specified for radio task");
        }

        if (elem.has("label")) {
            name = elem.get("label").getAsString();
        }

        executionCron = elem.get("execution_time").getAsString();

        JsonArray rawTasks = elem.get("tasks").getAsJsonArray();

        for (JsonElement e : rawTasks) {
            JsonObject obj = e.getAsJsonObject();

            TaskType type = gson.fromJson(obj.get("type"), TaskType.class);
            JsonObject rawParams = obj.has("params") ? obj.get("params").getAsJsonObject() : null;

            Map<Parameter, Object> paramMap = new HashMap<>();

            for (Parameter p : type.getParams()) {
                if (!p.isOptional() && (rawParams == null || !rawParams.has(p.getName()))) {
                    throw new JsonSyntaxException("Mandatory parameter '" + p.getName() + "' has no value");
                }
                paramMap.put(p, rawParams != null && rawParams.has(p.getName()) ? rawParams.get(p.getName()) : null);
            }

            tasks.add(new RadioTask(type.getExecutor(), new ParameterList(paramMap)));
        }
    }

    public List<RadioTask> getTasks() {
        return tasks;
    }

    public String getExecutionCron() {
        return executionCron;
    }

    public String getName() {
        return name;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
