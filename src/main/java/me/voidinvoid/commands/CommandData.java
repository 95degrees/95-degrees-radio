package me.voidinvoid.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;
import java.time.OffsetDateTime;

class CommandData {

    private final Member member;
    private final TextChannel textChannel;
    private final Message rawMessage;
    private final String[] args;
    private final Command command;
    private final String usedAlias;

    public CommandData(Member member, TextChannel textChannel, Message rawMessage, Command command, String usedAlias) {

        this.member = member;
        this.textChannel = textChannel;
        this.rawMessage = rawMessage;
        this.command = command;
        this.usedAlias = usedAlias;

        String rawString = rawMessage.getContentRaw();
        int argsIndex = rawString.indexOf(" ");

        this.args = argsIndex == -1 ? new String[]{} : getArgsString().split(" ");
    }

    public Member getMember() {
        return member;
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    public Message getRawMessage() {
        return rawMessage;
    }

    public String getArgsString() {
        String raw = rawMessage.getContentRaw();
        raw = raw.substring(Command.COMMAND_PREFIX.length() + usedAlias.length()).trim();

        return raw;
    }

    public String[] getArgs() {
        return args;
    }

    public void success(String message) {
        User user = member.getUser();

        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Command Successful")
                .setColor(Color.GREEN)
                .setDescription(message)
                .setFooter(user.getName(), user.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now()).build()).queue();
    }

    public void error(String message) {
        User user = member.getUser();

        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Command Error")
                .setColor(Color.RED)
                .setDescription(message)
                .appendDescription(command.getUsageMessage() == null ? "" : "\n\n`Usage: " + Command.COMMAND_PREFIX + usedAlias + " " + command.getUsageMessage() + "`")
                .setFooter(rawMessage.getContentDisplay(), user.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now()).build()).queue();
    }

    public String getUsedAlias() {
        return usedAlias;
    }
}
