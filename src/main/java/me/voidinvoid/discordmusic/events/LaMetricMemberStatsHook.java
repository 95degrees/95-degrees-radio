package me.voidinvoid.discordmusic.events;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LaMetricMemberStatsHook implements RadioService, EventListener {

    private static final String MEMBER_STATS_LOG_PREFIX = ConsoleColor.GREEN_BACKGROUND + " LAMETRIC STATS ";

    private URI UPDATE_URL;
    private java.net.http.HttpClient client;

    @Override
    public String getLogPrefix() {
        return MEMBER_STATS_LOG_PREFIX;
    }

    private String guildId;

    @Override
    public boolean canRun(RadioConfig config) {
        return !config.debug && config.useSocketServer;
    }

    @Override
    public void onLoad() {
        Guild guild = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat).getGuild();
        guildId = guild.getId();

        try {
            UPDATE_URL = new URI("https://developer.lametric.com/api/v1/dev/widget/update/com.lametric.c49d9f97e7e0da6213fbf0c68db60c8c/1");
        } catch (Exception e) {
            warn("LaMetric update URL is invalid!");
            e.printStackTrace();
        }

        client = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

        pushCount(guild.getMembers().size());
    }

    @Override
    public void onEvent(Event ev) {
        if (ev instanceof GuildMemberLeaveEvent || ev instanceof GuildMemberJoinEvent) {
            Guild g = ((GenericGuildMemberEvent) ev).getGuild();
            if (g.getId().equals(guildId)) {
                pushCount(g.getMembers().size());
            }
        }
    }

    private void pushCount(int count) {
        HttpClient http = HttpClientBuilder.create().build();

        try {

            HttpRequest req = HttpRequest.newBuilder(UPDATE_URL)
                    .header("Accept", "application/json")
                    .header("X-Access-Token", "NzAwZjE0YWNkYzI3YmQ0ZjNhZDFjYmNlYTE0MGQ1NmIwYjE1YWY1ZmFhYTEzZTY4MDdhYzNiYTYxMjNkY2E2Ng==")
                    .header("Cache-Control", "no-cache")
                    .POST(HttpRequest.BodyPublishers.ofString("{\n" +
                            "    \"frames\": [\n" +
                            "        {\n" +
                            "            \"text\": \"" + count + "\",\n" +
                            "            \"icon\": \"a1311\"\n" +
                            "        }\n" +
                            "    ]\n" +
                            "}")).build();

            client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> {
                if (r.statusCode() != 200) {
                    warn("Error updating LaMetric error: status code " + r.statusCode());
                }
            });

            log("Pushed member count at " + count);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
