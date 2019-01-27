package me.voidinvoid.discordmusic.dj.actions;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.advertisements.AdvertisementManager;
import me.voidinvoid.discordmusic.utils.Colors;
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
        return Radio.getInstance().getService(AdvertisementManager.class) != null;
    }

    @Override
    public void invoke(SongOrchestrator orch, AudioTrack track, TextChannel djChannel, User invoker) {
        Radio.getInstance().getService(AdvertisementManager.class).pushAdvertisement();

        djChannel.sendMessage(new EmbedBuilder()
                .setTitle("Queued Advert")
                .setColor(Colors.ACCENT_MAIN)
                .setDescription("An advert has been queued. It will play after this song")
                .setFooter(invoker.getName(), invoker.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now())
                .build()).queue();
    }
}