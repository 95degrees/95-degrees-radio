package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.utils.ChannelScope;

import java.util.List;
import java.util.stream.Collectors;

public class FindSongCommand extends Command {

    public FindSongCommand() {
        super("find-song", "Searches for a local song", "<song-name ...>", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();
        SongPlaylist active = Radio.instance.getOrchestrator().getActivePlaylist();

        if (args.length < 1) {
            data.error("Song search required");
            return;
        }

        String search = data.getArgsString().toLowerCase();
        List<Song> map = active.getSongs().getSongMap();
        List<Song> matches = map.stream().filter(s -> s.getLocation().toLowerCase().contains(search)).collect(Collectors.toList());

        if (matches.size() == 0) {
            data.error("No matches found from result");
            return;
        }

        String result = "[Song search]\n" + matches.size() + " match" + (matches.size() == 1 ? "" : "es") + "\n\n" + matches.stream().map(s -> "#" + (map.indexOf(s) + 1) + " - " + s.getLocation()).collect(Collectors.joining("\n"));
        data.getTextChannel().sendMessage("```" + result.substring(0, Math.min(1994, result.length())) + "```").queue();
    }
}
