package me.voidinvoid.dj.actions;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.songs.Song;
import net.dv8tion.jda.core.entities.TextChannel;

public class ToggleSuggestionsAction extends DJAction {

    public ToggleSuggestionsAction() {
        super("Toggle Suggestions", "📔");
    }

    @Override
    public void invoke(SongOrchestrator orch, Song song, TextChannel djChannel) {
        orch.setSuggestionsEnabled(!orch.areSuggestionsEnabled());
    }
}