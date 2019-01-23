package me.voidinvoid.discordmusic.levelling;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Colors;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.GuildVoiceState;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class LevellingManager implements EventListener {

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildVoiceJoinEvent || ev instanceof GuildVoiceMoveEvent) {
            GuildVoiceState vs = ((GenericGuildVoiceEvent) ev).getVoiceState();

            if (vs.inVoiceChannel() && ChannelScope.RADIO_VOICE.check(vs.getChannel())) { //joined vc

            }
        }
    }

    public void rewardExperience(User user, int amount) {
        TextChannel c = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat);

        c.sendMessage(new EmbedBuilder()
                .setTitle("Level Up")
                .setColor(Colors.ACCENT_LEVEL_UP)
                .setThumbnail("https://cdn.discordapp.com/attachments/505174503752728597/537703976032796712/todo.png")
                .setDescription(user.getAsMention() + " has levelled up!\n3 ➠ **4**")
                .addField("Reward", "<:degreecoin:431982714212843521> ?", false)
                .build()
        ).queue();
    }
}
