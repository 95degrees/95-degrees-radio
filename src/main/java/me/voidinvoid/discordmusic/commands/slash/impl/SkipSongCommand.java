package me.voidinvoid.discordmusic.commands.slash.impl;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.activity.ListeningContext;
import me.voidinvoid.discordmusic.commands.slash.CommandHandler;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandData;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandHandler;
import me.voidinvoid.discordmusic.events.SkipManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;

public class SkipSongCommand implements SlashCommandHandler {

    @CommandHandler
    public void skip(SlashCommandData data) {
        if (ChannelScope.DJ_CHAT.check(data.getEvent().getTextChannel())) {
            Radio.getInstance().getOrchestrator().playNextSong();

            data.success("Skipped to the next track");
            return;
        }


        if (ListeningContext.ALL.hasListener(data.getMember())) {
            Service.of(SkipManager.class).addSkipRequest(data.getMember().getUser(), data);
            return;
        }

        data.error("You must be listening to the radio to make skip requests");
    }

    @Override
    public CommandData getCommand() {
        return new CommandData("skip", "Votes to skip the current radio track");
    }

    @Override
    public boolean requiresDjAccess() {
        return false;
    }
}
