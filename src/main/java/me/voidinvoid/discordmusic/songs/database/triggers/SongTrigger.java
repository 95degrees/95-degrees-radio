package me.voidinvoid.discordmusic.songs.database.triggers;

import me.voidinvoid.discordmusic.tasks.ParameterList;
import org.bson.Document;

public class SongTrigger {

    private TriggerType type;
    private TriggerActivation on;
    private ParameterList params;

    public SongTrigger(TriggerType type, TriggerActivation on, ParameterList params) {

        this.type = type;
        this.on = on;
        this.params = params;
    }

    public TriggerType getType() {
        return type;
    }

    public TriggerActivation getOn() {
        return on;
    }

    public ParameterList getParams() {
        return params;
    }
}
