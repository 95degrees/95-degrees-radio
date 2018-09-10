package me.voidinvoid.commands;

import me.voidinvoid.DiscordRadio;

public class YouTubeSearchCommand extends Command {

    public YouTubeSearchCommand() {
        super("play", "Searches for a specified song on YouTube", "<search ...>", CommandScope.RADIO_AND_DJ_CHAT, "search");
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        if (args.length < 1) {
            data.error("YouTube search required");
            return;
        }

        //todo cleanup as part of orchestrator cleanup
        DiscordRadio.instance.getOrchestrator().addNetworkTrack(data.getUser(), data.getTextChannel(), "ytsearch:" + data.getArgsString(), CommandScope.DJ_CHAT.check(data.getTextChannel()), false, false, true);
    }
}
