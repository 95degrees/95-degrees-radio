package me.voidinvoid.dj.actions;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.songs.Song;
import net.dv8tion.jda.core.entities.TextChannel;

public abstract class DJAction {

    private final String name;
    private final String emoji;

    public DJAction(String name, String emoji) {

        this.name = name;
        this.emoji = emoji;
    }

    public String getName() {
        return name;
    }

    public String getEmoji() {
        return emoji;
    }

    public boolean shouldShow(Song song) {
        return true;
    }

    public abstract void invoke(SongOrchestrator orch, Song song, TextChannel djChannel);
}
