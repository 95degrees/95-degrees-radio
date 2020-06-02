package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import me.voidinvoid.discordmusic.utils.Emoji;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

public class CommandData {

    private final Member member;
    private final MessageChannel textChannel;
    private final Message rawMessage;
    private final String[] args;
    private final Command command;
    private final String usedAlias;
    private final boolean isConsole;
    private final String argsString;
    private String usageContext;

    public CommandData(Member member, MessageChannel textChannel, Message rawMessage, Command command, String usedAlias) {

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

    public MessageChannel getTextChannel() {
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

    public Member memberFromArgument(int index, boolean warnIfInvalid) {
        var mb = memberFromArgument(index);
        if (warnIfInvalid && mb == null) {
            error("Invalid user specified. You can @mention the user, or just enter their username to specify them");
        }

        return mb;
    }

    public Member memberFromArgument(int index) {
        if (index >= args.length || index < 0) return null;

        Guild g = Radio.getInstance().getGuild();
        String arg = args[index];

        try {
            if (arg.startsWith("<@!") && arg.endsWith(">")) { //handle nicknames
                return g.getMemberById(arg.substring(3, arg.length() - 1));
            } else if (arg.startsWith("<@") && arg.endsWith(">")) { //handle normal names
                return g.getMemberById(arg.substring(2, arg.length() - 1));
            } else if (arg.startsWith("\\<@") && arg.endsWith(">")) { //handle escaped names
                return g.getMemberById(arg.substring(3, arg.length() - 1));
            } else {
                try {
                    Member m = g.getMemberById(arg);
                    if (m != null) return m;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        arg = arg.toLowerCase();

        if (arg.length() < 3) return null;

        for (Member m : g.getMembers()) {
            var name = m.getUser().getName().toLowerCase();

            if (name.startsWith(arg)) return m;
        }

        return null;
    }

    public String getSentenceArgument(int index) {

        var joined = new StringBuilder();
        for (int i = index; i < args.length; i++) {
            joined.append(args[i]);
            joined.append(" ");
        }

        return joined.toString().strip();
    }

    public void embed(MessageEmbed embed) {
        if (textChannel == null) {
            System.out.println(embed.getDescription());
            return;
        }

        textChannel.sendMessage(embed).queue();
    }


    public MessageAction embedAction(MessageEmbed embed) {
        if (textChannel == null) {
            System.out.println(embed.getDescription());
            return null;
        }

        return textChannel.sendMessage(embed);
    }

    public void code(String message) {
        code(null, message);
    }

    public void code(String header, String message) {
        if (textChannel == null) {
            System.out.println(message);
            return;
        }

        textChannel.sendMessage((header == null ? "" : header + "\n") + "```" + message.substring(0, Math.min(1994 - (header == null ? 0 : header.length()), message.length())).replaceAll("`", "") + "```").queue();
    }

    public void success(String message) {
        success(message, -1);
    }

    public void success(String message, long displayMs) {
        if (member == null) {
            System.out.println(ConsoleColor.GREEN_BACKGROUND + " SUCCESS " + ConsoleColor.RESET_SPACE + message.replaceAll("`", ""));
            return;
        }

        User user = member.getUser();

        textChannel.sendMessage(new EmbedBuilder()
                .setTitle(Emoji.TICK + " Command Successful")
                .setColor(Color.GREEN)
                .setDescription(message)
                .setFooter(user.getName(), user.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now()).build())
                .queue(m -> {
                    if (displayMs > 0) {
                        m.delete().queueAfter(displayMs, TimeUnit.MILLISECONDS);
                    }
                });
    }

    public void sendMessage(String message) {
        sendMessage(message, -1);
    }

    public void sendMessage(String message, long displayMs) {
        if (member == null) {
            System.out.println(message);
            return;
        }

        textChannel.sendMessage(message).queue(m -> {
            if (displayMs > 0) m.delete().queueAfter(displayMs, TimeUnit.MILLISECONDS);
        });
    }

    public void mention(String message) {
        mention("", message);
    }

    public void mention(String prefix, String message) {
        mention(prefix, message, -1);
    }

    public void mention(String prefix, String message, long displayMs) {
        sendMessage((prefix == null || prefix.isEmpty() ? "" : prefix + " | ") + (member == null ? "Console" : "**" + member.getUser().getAsTag() + "**") + ", " + message, displayMs);
    }

    public MessageAction mentionAction(String prefix, String message) {
        String msg = (prefix == null || prefix.isEmpty() ? "" : prefix + " | ") + (member == null ? "Console" : "**" + member.getUser().getName() + "**") + ", " + message;
        if (textChannel == null) {
            System.out.println(msg);
            return null;
        }

        return textChannel.sendMessage(msg);
    }

    public void setUsageContext(String usageContext) {
        this.usageContext = usageContext;
    }

    public void error(String message) {
        error(message, -1);
    }

    public void error(String message, String usage) {

        var prev = usageContext;
        usageContext = usage;
        error(message, -1);
        usageContext = prev;
    }

    public void error(String message, long displayMs) {
        if (member == null) {
            System.out.println(ConsoleColor.RED_BACKGROUND + " ERROR " + ConsoleColor.RESET_SPACE + message.replaceAll("`", ""));
            return;
        }

        User user = member.getUser();

        textChannel.sendMessage(new EmbedBuilder()
                .setTitle(Emoji.CROSS + " Command Error")
                .setColor(Color.RED)
                .setDescription(message)
                .appendDescription(usageContext == null && command.getUsageMessage() == null ? "" : "\n\n`Usage: " + Command.COMMAND_PREFIX + usedAlias + " " + (usageContext == null ? command.getUsageMessage() : usageContext) + "`")
                .setFooter(rawMessage.getContentDisplay(), user.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now()).build())
                .queue(m -> {
                    if (displayMs > 0) {
                        m.delete().queueAfter(displayMs, TimeUnit.MILLISECONDS);
                    }
                });
    }

    public String getUsedAlias() {
        return usedAlias;
    }

    public boolean isConsole() {
        return isConsole;
    }
}
