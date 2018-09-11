package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.utils.ChannelScope;

import java.util.List;

public class QueueSongCommand extends Command {

    public QueueSongCommand() {
        super("queue-song", "Moves a song to the front of the queue (after network songs)", "<song#>", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();
        SongPlaylist active = Radio.instance.getOrchestrator().getActivePlaylist();

        if (args.length < 1) {
            data.error("Song number required. Use `!songs` to list songs");
            return;
        }

        int songId;
        try {
            songId = Integer.valueOf(args[0]);
        } catch (Exception ignored) {
            data.error("Invalid song number. Use `!songs` to list songs");
            return;
        }

        songId--;
        List<Song> map = active.getSongs().getSongMap();

        if (songId < 0 || songId >= map.size()) {
            data.error("Invalid song number. Use !songs to list songs");
            return;
        }

        Song song = map.get(songId);

        Radio.instance.getOrchestrator().getActivePlaylist().getSongs().moveSongToFront(song);
        data.success("Queued `" + song.getLocation().replace("`", "") + "`");
    }
}
