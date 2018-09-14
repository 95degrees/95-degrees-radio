package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.utils.ChannelScope;

public class SongsCommand extends Command {

    SongsCommand() {
        super("songs", "Lists all songs and their IDs", "[page#]", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        int page = 1;

        if (args.length >= 1) {

            try {
                page = Integer.valueOf(args[0]);
            } catch (Exception ignored) {
                data.error("Invalid page number");
                return;
            }
        }

        String map = Radio.instance.getOrchestrator().getActivePlaylist().getSongs().getFormattedMap(page);
        if (map == null) {
            data.error("Invalid page number");
            return;
        }

        data.getTextChannel().sendMessage("```" + map + "```").queue();
    }
}
