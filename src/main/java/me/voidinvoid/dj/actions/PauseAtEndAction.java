package me.voidinvoid.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.SongOrchestrator;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.time.OffsetDateTime;

public class PauseAtEndAction extends DJAction {

    public PauseAtEndAction() {
        super("Pause When Song Finishes", "🛑");
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker) {
        boolean paused = !orch.isPausePending();
        orch.setPausePending(paused);

        djChannel.sendMessage(new EmbedBuilder()
                .setTitle("Pause")
                .setDescription(paused ? "Now pausing after this song ends" : "No longer pausing after this song ends")
                .setFooter(invoker.getName(), invoker.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now())
                .build()).queue();
    }
}
