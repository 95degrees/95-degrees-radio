package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Rank;

public class SongsCommand extends Command {

    SongsCommand() {
        super("songs", "Lists all songs and their IDs", "[page#]", Rank.DJ);
    }

    @Override
    public void invoke(CommandData data) {
        Playlist active = Radio.getInstance().getOrchestrator().getActivePlaylist();

        if (!(active instanceof RadioPlaylist)) {
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

        String map = ((RadioPlaylist) active).getSongs().getFormattedMap(page);
        if (map == null) {
            data.error("Invalid page number");
            return;
        }

        data.code(map);
    }
}
