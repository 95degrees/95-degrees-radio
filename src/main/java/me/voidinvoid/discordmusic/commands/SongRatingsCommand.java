package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class SongRatingsCommand extends Command {

    SongRatingsCommand() {
        super("ratings", "Lists the ratings for the currently playing songs or all songs", "[all]", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);
        data.success("[todo]"); //todo
        data.success(db.getCollection("ratings").find().into(new ArrayList<>()).stream().map(d -> d.toJson()).collect(Collectors.joining(", ")));
    }
}
