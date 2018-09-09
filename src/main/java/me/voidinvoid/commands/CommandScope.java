package me.voidinvoid.commands;

import me.voidinvoid.config.RadioConfig;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.function.Function;

public enum CommandScope {
    RADIO_CHAT(t -> t.getId().equals(RadioConfig.config.channels.radioChat)),
    DJ_CHAT(t -> t.getId().equals(RadioConfig.config.channels.djChat)),
    RADIO_AND_DJ_CHAT(t -> t.getId().equals(RadioConfig.config.channels.radioChat) || t.getId().equals(RadioConfig.config.channels.djChat));

    private Function<TextChannel, Boolean> check;

    CommandScope(Function<TextChannel, Boolean> check) {

        this.check = check;
    }

    public boolean check(TextChannel channel) {
        return check.apply(channel);
    }
}
