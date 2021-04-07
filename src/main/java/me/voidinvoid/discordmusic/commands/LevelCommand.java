package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.levelling.LevellingManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.Emoji;
import me.voidinvoid.discordmusic.utils.Rank;
import net.dv8tion.jda.api.EmbedBuilder;
@Deprecated
public class LevelCommand extends Command {

    LevelCommand() {
        super("level", "Displays your current level", "[user]", null, false);
    }

    @Override
    public void invoke(CommandData data) {
        var lm = Radio.getInstance().getService(LevellingManager.class);

        var member = data.getArgs().length > 0 ? data.memberFromArgument(0, true) : data.getMember();
        if (member == null) return;

        var self = member.equals(data.getMember());

        var xp = lm.getExperience(member.getUser());

        var lvl = lm.getLevel(member.getUser());
        var next = lm.getNextLevel(lvl);

        var req = lm.getExperienceRequired(xp);

        var embed = new EmbedBuilder()
                .setAuthor(member.getUser().getName(), null, member.getUser().getEffectiveAvatarUrl())
                .setTitle("Radio Levelling")
                .setColor(Colors.ACCENT_MAIN)
                .setThumbnail(Radio.getInstance().getJda().getSelfUser().getEffectiveAvatarUrl())
                .setDescription((self ? "You are" : member.getUser().getName() + " is") + " currently level **" + lvl.getLevel() + "**")
                .addField("Next Level", "[todo] / "  + " xp (" + /*DECIMAL_FORMAT.format(levelPercent * 100)*/ "%)", true)
                .addField("Total Experience", xp + " xp", true);

        data.embed(embed.build());

        //data.mention("🎵", (self ? "you are" : "**" + member.getUser().getAsTag() + "** is") + " currently level **" + lvl.getLevel() + "**!\n\n" + (next == null ? "This is the max level! " + (self ? "Thanks for listening! " : "") + "<:radioislit:489942546903203851>" : (self ? "You" : "They") + " are **" + req + "** xp away from levelling up to level **" + next.getLevel() + "**! **1** xp is earned for every minute listening to the radio!"));
    }
}
