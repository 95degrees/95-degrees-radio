package me.voidinvoid.discordmusic.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

public class SkipSongAction extends DJAction {

    public SkipSongAction() {
        super("Skip Song", "⏩", RPCSocketManager.CLIENT_CONTROL_SKIP_SONG, 0);
    }

    @Override
    public String invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker, ButtonClickEvent event) {
        orch.playNextSong();

        return "Skipped song";
    }
}
