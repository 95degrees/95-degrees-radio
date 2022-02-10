package me.voidinvoid.discordmusic.interactions;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public class ButtonEvent {

    private final ButtonClickEvent event;
    private final ButtonData data;

    public ButtonEvent(ButtonClickEvent event, ButtonData data) {

        this.event = event;
        this.data = data;
    }

    public ButtonClickEvent getEvent() {
        return event;
    }

    public ButtonData getData() {
        return data;
    }
}
