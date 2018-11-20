package me.voidinvoid.discordmusic.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.SongOrchestrator;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

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

    public boolean shouldShow(AudioTrack track) {
        return true;
    }

    public abstract void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker);
}
