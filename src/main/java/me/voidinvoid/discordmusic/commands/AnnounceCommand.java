package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Colors;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.awt.*;
import java.time.OffsetDateTime;

public class AnnounceCommand extends Command {

    AnnounceCommand() {
        super("radio-announce", "Announces a message to radio and DJ channels", "[hex-colour] <announcement ...>", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String[] args = data.getArgs();

        if (args.length < 1) {
            data.error("Hex colour and/or announcement content required\nhttps://www.google.co.uk/search?q=colour+picker");
            return;
        }

        Color colour = Colors.ACCENT_ANNOUNCEMENT;

        String announcement = data.getArgsString();

        if (args[0].startsWith("#")) {
            try {
                colour = Color.decode(args[0].toUpperCase());

                announcement = announcement.substring(args[0].length()).trim();
            } catch (Exception ignored) {
                data.error("Hex colour format is invalid\nhttps://www.google.co.uk/search?q=colour+picker");
                return;
            }
        }

        MessageEmbed embed = new EmbedBuilder().setTitle("Announcement").setDescription(announcement).setTimestamp(OffsetDateTime.now()).setColor(colour).build();

        Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.djChat).sendMessage(embed).queue();
        Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage(embed).queue();
    }
}
