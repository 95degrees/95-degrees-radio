package me.voidinvoid.discordmusic.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class ToggleSuggestionsAction extends DJAction {

    public ToggleSuggestionsAction() {
        super("Toggle Suggestions", "📔", RPCSocketManager.CLIENT_CONTROL_TOGGLE_SUGGESTIONS);
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker) {
        orch.setSuggestionsEnabled(!orch.areSuggestionsEnabled(), invoker);
    }
}