package me.voidinvoid.commands;

import me.voidinvoid.DiscordRadio;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.songs.SongPlaylist;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.stream.Collectors;

public class PlaylistsCommand extends Command {

    public PlaylistsCommand() {
        super("playlists", "Lists all playlists", null, CommandScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        SongPlaylist active = DiscordRadio.instance.getOrchestrator().getActivePlaylist();
        data.getTextChannel().sendMessage("```[Playlists]\n\n" + DiscordRadio.instance.getOrchestrator().getPlaylists().stream().map(p -> p.getName() + (active.equals(p) ? " (ACTIVE) [" : " [") + p.getInternal() + "]").collect(Collectors.joining("\n")) + "```").queue();
    }
}
