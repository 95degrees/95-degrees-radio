package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.songs.Playlist;
import me.voidinvoid.songs.Song;
import me.voidinvoid.songs.SongPlaylist;
import me.voidinvoid.utils.ChannelScope;

import java.util.List;

public class PlaySongCommand extends Command {

    PlaySongCommand() {
        super("play-song", "Plays a song in the current playlist", "<song#>", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        Playlist active = Radio.instance.getOrchestrator().getActivePlaylist();

        if (!(active instanceof SongPlaylist)) {
            data.error("This command can only be used when a song playlist is active");
            return;
        }

        String[] args = data.getArgs();

        if (args.length < 1) {
            data.error("Song number required. Use `!songs` to list songs");
            return;
        }

        int song;
        try {
            song = Integer.valueOf(args[0]);
        } catch (Exception ignored) {
            data.error("Invalid song number. Use `!songs` to list songs");
            return;
        }

        song--;
        List<Song> map = ((SongPlaylist) active).getSongs().getSongMap();

        if (song < 0 || song >= map.size()) {
            data.error("Invalid song number. Use !songs to list songs");
            return;
        }

        Radio.instance.getOrchestrator().playSong(map.get(song));
    }
}
