package me.voidinvoid.discordmusic.interactions;

import java.util.function.Consumer;

public class ButtonData {

    private final String baseId;
    private String customData;
    private boolean dirty;
    private Consumer<ButtonEvent> handler;

    public ButtonData(String baseId, Consumer<ButtonEvent> handler) {

        this.baseId = baseId;
        this.handler = handler;
    }

    public String getCustomData() {
        return customData;
    }

    public ButtonData setCustomData(String customData) {
        this.customData = customData;
        dirty = true;
        return this;
    }

    public String getBaseId() {
        return baseId;
    }

    public String getFullId() {
        return baseId + ":" + customData;
    }

    protected boolean isDirty() {
        return dirty;
    }

    protected ButtonData setDirty(boolean dirty) {
        this.dirty = dirty;
        return this;
    }

    public Consumer<ButtonEvent> getHandler() {
        return handler;
    }
}
