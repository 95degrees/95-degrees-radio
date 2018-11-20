package me.voidinvoid.discordmusic.tasks.types;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.tasks.RadioTaskExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

public class AnnounceTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        String message = params.get("message", String.class);
        boolean announceToDj = params.get("announce_to_dj_channel", Boolean.class);
        boolean announceToText = params.get("announce_to_text_channel", Boolean.class);
        String title = params.get("title", String.class);
        String additionalChannel = params.get("announce_to_channel", String.class);
        int colour = params.get("colour", Integer.class);
        long deleteAfter = params.get("delete_after", Long.class);
        String image = params.get("image_url", String.class);

        MessageEmbed embed = new EmbedBuilder().setTitle(title).setDescription(message).setImage(image).setTimestamp(OffsetDateTime.now()).setColor(colour).build();

        if (announceToDj)
            Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.djChat).sendMessage(embed).queue();
        if (announceToText)
            Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage(embed).queue();
        if (additionalChannel != null) {
            TextChannel channel = Radio.instance.getJda().getTextChannelById(additionalChannel);

            if (channel != null) {
                channel.sendMessage(embed).queue(m -> {
                    if (deleteAfter > 0) {
                        m.delete().queueAfter(deleteAfter, TimeUnit.SECONDS);
                    }
                });
            }
        }
    }
}
