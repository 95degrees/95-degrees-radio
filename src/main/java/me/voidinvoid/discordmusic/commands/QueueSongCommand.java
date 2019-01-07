package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.Playlist;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.utils.ChannelScope;

import java.util.List;

public class QueueSongCommand extends Command {

    QueueSongCommand() {
        super("queue-song", "Moves a song to the front of the queue (after network songs)", "<song#>", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();
        Playlist active = Radio.getInstance().getOrchestrator().getActivePlaylist();

        if (!(active instanceof RadioPlaylist)) {
            data.error("This command can only be used when a song playlist is active");
            return;
        }

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
        List<Song> map = ((RadioPlaylist) active).getSongs().getSongMap();

        if (songId < 0 || songId >= map.size()) {
            data.error("Invalid song number. Use !songs to list songs");
            return;
        }

        Song song = map.get(songId);

        ((RadioPlaylist) active).getSongs().moveSongToFront(song);
        data.success("Queued `" + song.getFileName().replace("`", "") + "`");
    }
}
