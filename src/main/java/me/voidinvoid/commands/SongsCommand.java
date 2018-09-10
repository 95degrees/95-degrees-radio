package me.voidinvoid.commands;

import me.voidinvoid.DiscordRadio;
import me.voidinvoid.songs.SongPlaylist;

import java.util.stream.Collectors;

public class SongsCommand extends Command {

    public SongsCommand() {
        super("songs", "Lists all songs and their IDs", "[page#]", CommandScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        int page = 0;

        if (args.length >= 1) {

            try {
                page = Integer.valueOf(args[0]);
            } catch (Exception ignored) {
                data.error("Invalid page number");
                return;
            }
        }

        String map = DiscordRadio.instance.getOrchestrator().getActivePlaylist().getSongs().getFormattedMap(page);
        if (map == null) {
            data.error("Invalid page number");
            return;
        }

        data.getTextChannel().sendMessage("```" + map + "```").queue();
     }
}
