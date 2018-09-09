package me.voidinvoid.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.SongOrchestrator;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class ToggleSuggestionsAction extends DJAction {

    public ToggleSuggestionsAction() {
        super("Toggle Suggestions", "📔");
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker) {
        orch.setSuggestionsEnabled(!orch.areSuggestionsEnabled(), invoker);
    }
}