package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Rank;
import me.voidinvoid.discordmusic.utils.Songs;

import java.util.List;
import java.util.stream.Collectors;

public class FindSongCommand extends Command {

    FindSongCommand() {
        super("find-song", "Searches for a local song", "<song-name ...>", Rank.DJ);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();
        Playlist active = Radio.getInstance().getOrchestrator().getActivePlaylist();

        if (!(active instanceof RadioPlaylist)) return;

        if (args.length < 1) {
            data.error("Song search required");
            return;
        }

        String search = data.getArgsString().toLowerCase();
        List<Song> map = ((RadioPlaylist) active).getSongs().getSongMap();
        List<Song> queue = ((RadioPlaylist) active).getSongs().getQueue();
        List<Song> matches = map.stream().filter(s -> Songs.titleArtist(s).toLowerCase().contains(search)).collect(Collectors.toList());

        if (matches.size() == 0) {
            data.error("No matches found from result");
            return;
        }

        String result = "[Song Search]\n" + matches.size() + " match" + (matches.size() == 1 ? "" : "es") + "\n\n" + matches.stream().map(s -> "#" + (map.indexOf(s) + 1) + " [Queue " + (queue.indexOf(s) + 1) + "/" + queue.size() + "] " + Songs.titleArtist(s)).collect(Collectors.joining("\n"));

        data.code(result);
    }
}
