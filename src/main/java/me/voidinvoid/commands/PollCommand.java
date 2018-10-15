package me.voidinvoid.commands;

import me.voidinvoid.Radio;
import me.voidinvoid.config.RadioConfig;
import me.voidinvoid.utils.ChannelScope;
import me.voidinvoid.utils.Colors;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PollCommand extends Command {

    private static final List<String> NUMBER_EMOJIS = Arrays.asList(new String(new char[]{49, 8419}), new String(new char[]{50, 8419}), new String(new char[]{51, 8419}), new String(new char[]{52, 8419}), new String(new char[]{53, 8419}), new String(new char[]{54, 8419}), new String(new char[]{55, 8419}), new String(new char[]{56, 8419}), new String(new char[]{57, 8419}), "🔟");

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

        String question = args.split("")[0];
        String[] answers = args.substring(question.length()).split("\\|");

        MessageEmbed embed = new EmbedBuilder().setTitle("Poll").setDescription("**" + question + "**\n\n" + IntStream.range(0, answers.length).mapToObj(i -> NUMBER_EMOJIS.get(i) + " " + answers[i]).collect(Collectors.joining("\n"))).setTimestamp(OffsetDateTime.now()).setColor(Colors.ACCENT_POLL).build();

        Radio.instance.getJda().getTextChannelById(RadioConfig.config.channels.radioChat).sendMessage(embed).queue();

        data.success("Created a poll");
    }
}
