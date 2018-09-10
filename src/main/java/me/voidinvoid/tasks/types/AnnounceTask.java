package me.voidinvoid.tasks.types;

import me.voidinvoid.SongOrchestrator;
import me.voidinvoid.tasks.ParameterList;
import me.voidinvoid.tasks.RadioTaskExecutor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.time.OffsetDateTime;

public class AnnounceTask extends RadioTaskExecutor {

    @Override
    public void runTask(SongOrchestrator orch, ParameterList params) {
        String message = params.get("message", String.class);
        boolean announceToDj = params.get("announce_to_dj_channel", Boolean.class);
        boolean announceToText = params.get("announce_to_text_channel", Boolean.class);
        int colour = params.get("colour", Integer.class);

        MessageEmbed embed = new EmbedBuilder().setTitle("Announcement").setDescription(message).setTimestamp(OffsetDateTime.now()).setColor(colour).build();

        if (announceToDj) orch.djChannel_temp.sendMessage(embed).queue();
        //todo if (announceToText) orch.getRadioChannel().sendMessage(embed).queue();
    }
}
