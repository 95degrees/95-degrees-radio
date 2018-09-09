package me.voidinvoid.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.SongOrchestrator;
import net.dv8tion.jda.core.entities.TextChannel;

public class ToggleSuggestionsAction extends DJAction {

    public ToggleSuggestionsAction() {
        super("Toggle Suggestions", "📔");
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel) {
        orch.setSuggestionsEnabled(!orch.areSuggestionsEnabled());
    }
}