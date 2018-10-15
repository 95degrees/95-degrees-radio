package me.voidinvoid.tasks;

public class Parameter {

    private String name;
    private Object defaultValue;
    private boolean optional;

    public Parameter(String name, Object defaultValue) {

        this.name = name;
        this.defaultValue = defaultValue;
        this.optional = true;
    }

    public Parameter(String name) {
        this(name, null);
        this.optional = true;
    }

    public static Parameter of(String name) {
        return new Parameter(name);
    }

    public static Parameter of(String name, Object defaultValue) {
        return new Parameter(name, defaultValue);
    }

    public String getName() {
        return name;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public boolean isOptional() {
        return optional;
    }
}
