package me.voidinvoid.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.Map;

public class ParameterList {

    private final static Gson GSON = new Gson();

    private Map<Parameter, Object> params;

    public ParameterList(Map<Parameter, Object> params) {
        this.params = params;
    }

    public <T> T get(String name, Class<T> type) {
        for (Parameter m : params.keySet()) {
            if (m.getName().equalsIgnoreCase(name)) {
                Object value = params.get(m) == null ? m.getDefaultValue() : params.get(m);
                if (value instanceof JsonElement) return GSON.fromJson((JsonElement) value, type);
                return type.cast(value);
            }
        }

        return null;
    }
}