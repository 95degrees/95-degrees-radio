package me.voidinvoid.discordmusic.commands.slash.impl;

import me.voidinvoid.discordmusic.commands.slash.CommandHandler;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandData;
import me.voidinvoid.discordmusic.commands.slash.SlashCommandHandler;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.restream.RadioRestreamManager;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class RestreamCommand implements SlashCommandHandler {

    @CommandHandler("join")
    public void join(SlashCommandData data) {
        if (data.getMember().getVoiceState() == null || data.getMember().getVoiceState().getChannel() == null) {
            data.error("You must be in a voice channel to use Restream!", true);
            return;
        }

        var channel = data.getMember().getVoiceState().getChannel();

        if (channel.getId().equals(RadioConfig.config.channels.voice)) {
            data.error("You cannot use Restream within the radio voice channel!", true);
            return;
        }

        Service.of(RadioRestreamManager.class).joinVoiceChannel(channel.getId());
        data.success("Enabled Restream within " + channel.getName());
    }

    @CommandHandler("leave")
    public void leave(SlashCommandData data) {
        if (data.getMember().getVoiceState() == null || data.getMember().getVoiceState().getChannel() == null) {
            data.error("You must be in a voice channel to use Restream!", true);
            return;
        }

        var channel = data.getMember().getVoiceState().getChannel();
        var restream = Service.of(RadioRestreamManager.class);

        var res = restream.leaveVoice(data.getMember().getGuild().getId());
        if (!res) {
            data.error("Restream is not in your current voice channel!", true);
            return;
        }

        data.success("Removed Restream from " + channel.getName());
    }

    @Override
    public CommandData getCommand() {
        return new CommandData("restream", "Enables the Restream system, which can clone the radio into other voice channels")
                .addSubcommand(new SubcommandData("join", "Joins your voice channel"))
                .addSubcommand(new SubcommandData("leave", "Disconnects the Restream bot"));
    }

    @Override
    public boolean requiresDjAccess() {
        return false;
    }
}
