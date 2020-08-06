package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.Colors;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import me.voidinvoid.discordmusic.utils.Emoji;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CommandData {

    public static final String ERROR_SPECIFY_USER = "You must specify a user to target";

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
        if (args.length - 1 < index) {
            if (warnIfInvalid) {
                error(ERROR_SPECIFY_USER);
            }

            return null;
        }

        var mb = memberFromArgument(index);
        if (warnIfInvalid && mb == null) {
            error("Invalid user specified. You can @mention the user, or just enter their username to specify them");
        }

        return mb;
    }

    public User userFromArgument(int index) {
        if (index >= args.length || index < 0) return null;

        var jda = Radio.getInstance().getJda();
        String arg = args[index];

        try {
            var u = jda.retrieveUserById(arg).complete();
            if (u != null) return u;
        } catch (Exception ignored) {
        }

        try {
            var u = jda.getUserByTag(arg);
            if (u != null) return u;
        } catch (Exception ignored) {
        }

        var mb = memberFromArgument(index);
        if (mb != null) return mb.getUser();

        return null;
    }

    public Member memberFromArgument(int index) {
        if (index >= args.length || index < 0) return null;

        Guild g = Radio.getInstance().getGuild();
        String arg = args[index];

        try {
            if (arg.startsWith("<@!") && arg.endsWith(">")) { //handle nicknames
                return g.retrieveMemberById(arg.substring(3, arg.length() - 1)).onErrorMap(t -> null).complete();
            } else if (arg.startsWith("<@") && arg.endsWith(">")) { //handle normal names
                return g.retrieveMemberById(arg.substring(2, arg.length() - 1)).onErrorMap(t -> null).complete();
            } else if (arg.startsWith("\\<@") && arg.endsWith(">")) { //handle escaped names
                return g.retrieveMemberById(arg.substring(3, arg.length() - 1)).onErrorMap(t -> null).complete();
            } else {
                Member m = g.retrieveMemberById(arg).onErrorMap(t -> null).complete();
                if (m != null) return m;
            }
        } catch (Exception ignored) {
        }

        arg = arg.toLowerCase();

        if (arg.length() < 3) return null;

        //HACK: load the member list for *next time* at least. can't use get() here
        g.loadMembers().onSuccess(s -> {});

        for (Member m : g.getMembers()) {
            var name = m.getUser().getName().toLowerCase();

            if (name.startsWith(arg)) return m;
        }

        return null;
    }

    public TextChannel textChannelFromArgument(int index) {
        if (index >= args.length || index < 0) return null;

        Guild g = Radio.getInstance().getGuild();
        String arg = args[index];

        try { //<#505174503752728597>
            var tc = g.getTextChannelById(arg);
            if (tc != null) return tc;

            if (arg.startsWith("<#") && arg.endsWith(">")) {
                tc = g.getTextChannelById(arg.substring(2, arg.length() - 1));
                if (tc != null) return tc;
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    public Integer intFromArgument(int index) {
        if (index >= args.length || index < 0) return null;

        try {
            return Integer.parseInt(args[index]);
        } catch (Exception ignored) {
            return null;
        }
    }

    public Long longFromArgument(int index) {
        if (index >= args.length || index < 0) return null;

        try {
            return Long.parseLong(args[index]);
        } catch (Exception ignored) {
            return null;
        }
    }

    public String getSentenceArgument(int index) {

        var joined = new StringBuilder();
        for (int i = index; i < args.length; i++) {
            joined.append(args[i]);
            joined.append(" ");
        }

        return joined.toString().strip();
    }

    public boolean checkForArgument(int argumentNumber, String errorMessage, String... arguments) {
        if (args.length > argumentNumber) {
            var arg = args[argumentNumber].toLowerCase();

            for (var a : arguments) {
                if (a.equalsIgnoreCase(arg)) return true;
            }
        }

        if (errorMessage != null) {
            error(errorMessage);
        }

        return false;
    }

    public void embed(String content, MessageEmbed embed) {
        if (textChannel == null) {
            System.out.println(content);
            System.out.println(embed.getDescription());
            return;
        }

        textChannel.sendMessage(content).embed(embed).queue();
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
                .setColor(Colors.ACCENT_MAIN)
                .setDescription(Emoji.TICK + " " + message)
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

    public void prefix(String prefix, String message) {
        sendMessage(prefix + " • " + message);
    }

    public void mention(String message) {
        mention("", message);
    }

    public void mention(String prefix, String message) {
        mention(prefix, message, -1);
    }

    public void mention(String prefix, String message, long displayMs) {
        sendMessage((prefix == null || prefix.isEmpty() ? "" : prefix + " • ") + (member == null ? "Console" : "**" + member.getUser().getName() + "**") + ", " + message, displayMs);
    }

    public MessageAction mentionAction(String prefix, String message) {
        String msg = (prefix == null || prefix.isEmpty() ? "" : prefix + " • ") + (member == null ? "Console" : "**" + member.getUser().getName() + "**") + ", " + message;
        if (textChannel == null) {
            System.out.println(msg);
            return null;
        }

        return textChannel.sendMessage(msg);
    }

    public void setUsageContext(String usageContext) {
        this.usageContext = usageContext;
    }

    public void inDevelopment() {
        error("This feature is currently in development. Stay tuned!");
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

        textChannel.sendMessage(createErrorEmbed(message).build())
                .queue(m -> {
                    if (displayMs > 0) {
                        m.delete().queueAfter(displayMs, TimeUnit.MILLISECONDS);
                    }
                });
    }

    public EmbedBuilder createErrorEmbed(String message) {
        var embed = new EmbedBuilder()
                .setTitle(Emoji.CROSS + " Command Error")
                .setColor(Color.RED)
                .setDescription(message)
                .setFooter(rawMessage.getContentDisplay(), member.getUser().getAvatarUrl())
                .setTimestamp(OffsetDateTime.now());

        if (usageContext != null || command.getUsageMessage() != null) {
            embed.addField("Command Usage", "`" + Command.COMMAND_PREFIX + usedAlias + " " + (usageContext == null ? command.getUsageMessage() : usageContext) + "`", true);
        }

        return embed;
    }

    public String getUsedAlias() {
        return usedAlias;
    }

    public boolean isConsole() {
        return isConsole;
    }
}