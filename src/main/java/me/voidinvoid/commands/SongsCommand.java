package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.songs.Playlist;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.utils.ChannelScope;

public class SongsCommand extends Command {

    SongsCommand() {
        super("songs", "Lists all songs and their IDs", "[page#]", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Playlist active = Radio.instance.getOrchestrator().getActivePlaylist();

        if (!(active instanceof SongPlaylist)) {
            data.error("This command can only be used when a song playlist is active");
            return;
        }

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

        String map = ((SongPlaylist) active).getSongs().getFormattedMap(page);
        if (map == null) {
            data.error("Invalid page number");
            return;
        }

        data.getTextChannel().sendMessage("```" + map + "```").queue();
    }
}
