package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.ChannelScope;
import me.voidinvoid.discordmusic.utils.Formatting;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PollCommand extends Command {

    PollCommand() {
        super("radio-poll", "Creates a poll in the text channel", "<poll description ...>,[option a...]|[option b...] ...", ChannelScope.DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        String args = data.getArgsString();

        if (args.length() == 0 || !args.contains(",")) {
            data.error("Poll description and format required\nFor example: `Favourite colour?,Green|Red|Light blue`");
            return;
        }

        String question = args.split(",")[0];
        String[] answers = args.substring(question.length() + 1).split("\\|");

        MessageEmbed embed = new EmbedBuilder().setTitle("Poll").setDescription("**" + question.trim() + "**\nReact with the corresponding number to cast your vote!\n\n" + IntStream.range(0, answers.length).mapToObj(i -> Formatting.NUMBER_EMOTES.get(i) + " " + answers[i].trim()).collect(Collectors.joining("\n"))).setTimestamp(OffsetDateTime.now()).setColor(Colors.ACCENT_POLL).build();

        Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage(embed).queue(m -> {
            for (int i = 0; i < answers.length; i++) {
                m.addReaction(Formatting.NUMBER_EMOTES.get(i)).queue();
            }
        });

        data.success("Created a poll");
    }
}
