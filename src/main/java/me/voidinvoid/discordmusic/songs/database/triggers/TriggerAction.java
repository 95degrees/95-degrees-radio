package me.voidinvoid.discordmusic.songs.database.triggers;

import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.tasks.ParameterList;

public interface TriggerAction {

    void onTrigger(Song song, ParameterList params);
}
