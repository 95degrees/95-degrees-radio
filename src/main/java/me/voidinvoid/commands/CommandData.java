package me.voidinvoid.commands;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;
import java.time.OffsetDateTime;

public class CommandData {

    private final User user;
    private final TextChannel textChannel;
    private final Message rawMessage;
    private final String[] args;

    public CommandData(User user, TextChannel textChannel, Message rawMessage) {

        this.user = user;
        this.textChannel = textChannel;
        this.rawMessage = rawMessage;

        String rawString = rawMessage.getContentRaw();
        int argsIndex = rawString.indexOf(" ");

        this.args = argsIndex == -1 ? new String[] {} : rawMessage.getContentRaw().substring(argsIndex, rawString.length()).split(" ");
    }

    public User getUser() {
        return user;
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    public Message getRawMessage() {
        return rawMessage;
    }

    public String[] getArgs() {
        return args;
    }

    public void success(String message) {
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Command Successful")
                .setColor(Color.GREEN)
                .setDescription(message)
                .setFooter(user.getName(), user.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now()).build()).queue();
    }

    public void error(String message) {
        textChannel.sendMessage(new EmbedBuilder()
                .setTitle("Command Error")
                .setColor(Color.RED)
                .setDescription(message)
                .setFooter(rawMessage.getContentStripped(), user.getAvatarUrl())
                .setTimestamp(OffsetDateTime.now()).build()).queue();
    }
}
