package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.activity.ListeningContext;
import me.voidinvoid.discordmusic.events.SkipManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.entities.GuildChannel;

public class SkipCommand extends Command {

    SkipCommand() {
        super("skip", "Skips the currently playing track", null, null);
    }

    @Override
    public void invoke(CommandData data) {
        if (data.getTextChannel() == null || ChannelScope.DJ_CHAT.check((GuildChannel) data.getTextChannel())) {
            Radio.getInstance().getOrchestrator().playNextSong();

            data.success("Skipped to the next track");
            return;
        }

        if (ListeningContext.ALL.hasListener(data.getMember())) {
            Service.of(SkipManager.class).addSkipRequest(data.getMember().getUser(), null);
            return;
        }

        data.error("You must be listening to the radio to make skip requests");
    }
}
