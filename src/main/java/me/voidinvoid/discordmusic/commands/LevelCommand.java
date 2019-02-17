package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.levelling.LevellingManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;

public class LevelCommand extends Command {

    LevelCommand() {
        super("level", "Displays your current level", null, ChannelScope.RADIO_AND_DJ_CHAT, false);
    }

    @Override
    public void invoke(CommandData data) {
        var lm = Radio.getInstance().getService(LevellingManager.class);

        var xp = lm.getExperience(data.getMember().getUser());

        var lvl = lm.getLevel(data.getMember().getUser());
        var next = lm.getNextLevel(lvl);

        var req = lm.getExperienceRequired(xp);

        data.success("You are currently level **" + lvl.getLevel() + "**!\n\n" + (next == null ? "This is the max level! Thanks for listening! <:radioislit:489942546903203851>" : "You are **" + req + "** xp away from levelling up to level **" + next.getLevel() + "**! You earn **1** xp for listening to the radio every minute!"));
    }
}
