package me.voidinvoid.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.Radio;
import me.voidinvoid.SongOrchestrator;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.time.OffsetDateTime;

public class PlayAdvertAction extends DJAction {

    public PlayAdvertAction() {
        super("Queue Advert", "📰");
    }

    @Override
    public boolean shouldShow(AudioTrack track) {
        return Radio.instance.getAdvertisementManager() != null;
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker) {
        Radio.instance.getAdvertisementManager().pushAdvertisement();

        djChannel.sendMessage(new EmbedBuilder()
                .setTitle("Queued Advert")
                .setDescription("An advert has been queued. It will play after this song")
                .setFooter(invoker.getName(), invoker.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now())
                .build()).queue();
    }
}