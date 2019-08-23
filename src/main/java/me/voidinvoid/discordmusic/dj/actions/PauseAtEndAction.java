package me.voidinvoid.discordmusic.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.utils.Colors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

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
                .setColor(Colors.ACCENT_MAIN)
                .setDescription(paused ? "⏸ Now pausing after this song ends" : "▶ No longer pausing after this song ends")
                .setFooter(invoker.getName(), invoker.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now())
                .build()).queue(m -> m.delete().queueAfter(10, TimeUnit.SECONDS));
    }
}
