package me.voidinvoid.discordmusic.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.SongOrchestrator;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public abstract class DJAction {

    private final String name;
    private final String emoji;
    private final String socketCode;
    private final int actionRowIndex;

    public DJAction(String name, String emoji, String socketCode, int actionRowIndex) {

        this.name = name;
        this.emoji = emoji;
        this.socketCode = socketCode;
        this.actionRowIndex = actionRowIndex;
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

    public String getSocketCode() {
        return socketCode;
    }

    public int getActionRowIndex() {
        return actionRowIndex;
    }

    public abstract String invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker, ButtonClickEvent event);
}
