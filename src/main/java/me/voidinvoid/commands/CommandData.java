package me.voidinvoid.commands;

import me.voidinvoid.utils.ConsoleColor;
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
    private final boolean isConsole;
    private final String argsString;

    public CommandData(Member member, TextChannel textChannel, Message rawMessage, Command command, String usedAlias) {

        this.member = member;
        this.textChannel = textChannel;
        this.rawMessage = rawMessage;
        this.command = command;
        this.usedAlias = usedAlias;

        isConsole = false;

        String rawString = rawMessage.getContentRaw();
        int argsIndex = rawString.indexOf(" ");

        argsString = rawString.substring(Command.COMMAND_PREFIX.length() + usedAlias.length()).trim();

        this.args = argsIndex == -1 ? new String[]{} : argsString.split(" ");
    }

    public CommandData(String rawString, Command command, String usedAlias) {

        this.member = null;
        this.textChannel = null;
        this.rawMessage = null;
        this.command = command;
        this.usedAlias = usedAlias;

        isConsole = true;

        int argsIndex = rawString.indexOf(" ");

        argsString = rawString.substring(usedAlias.length()).trim();

        this.args = argsIndex == -1 ? new String[]{} : argsString.split(" ");
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
        return argsString;
    }

    public String[] getArgs() {
        return args;
    }

    public void code(String message) {
        if (textChannel == null) {
            System.out.println(message);
            return;
        }

        textChannel.sendMessage("```" + message.substring(0, Math.min(1994, message.length())).replaceAll("`", "") + "```").queue();
    }

    public void success(String message) {
        if (member == null) {
            System.out.println(ConsoleColor.GREEN_BACKGROUND + " SUCCESS " + ConsoleColor.RESET_SPACE + message.replaceAll("`", ""));
            return;
        }

        User user = member.getUser();

        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Command Successful")
                .setColor(Color.GREEN)
                .setDescription(message)
                .setFooter(user.getName(), user.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now()).build()).queue();
    }

    public void error(String message) {
        if (member == null) {
            System.out.println(ConsoleColor.RED_BACKGROUND + " ERROR " + ConsoleColor.RESET_SPACE + message.replaceAll("`", ""));
            return;
        }

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

    public boolean isConsole() {
        return isConsole;
    }
}
