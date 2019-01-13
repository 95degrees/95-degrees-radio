package me.voidinvoid.discordmusic.events;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.config.RadioConfig;
import me.voidinvoid.discordmusic.utils.ConsoleColor;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

public class LaMetricMemberStatsHook implements EventListener {

    private static final String MEMBER_STATS_LOG_PREFIX = ConsoleColor.GREEN_BACKGROUND + " LAMETRIC STATS " + ConsoleColor.RESET_SPACE;

    private String guildId;

    public LaMetricMemberStatsHook() {
        Guild guild = Radio.getInstance().getJda().getTextChannelById(RadioConfig.config.channels.radioChat).getGuild();
        guildId = guild.getId();

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

            HttpPost request = new HttpPost("https://developer.lametric.com/api/v1/dev/widget/update/com.lametric.c49d9f97e7e0da6213fbf0c68db60c8c/1");
            StringEntity params = new StringEntity("{\n" +
                    "    \"frames\": [\n" +
                    "        {\n" +
                    "            \"text\": \"" + count + "\",\n" +
                    "            \"icon\": \"a1311\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}");
            request.addHeader("Accept", "application/json");
            request.addHeader("X-Access-Token", "NzAwZjE0YWNkYzI3YmQ0ZjNhZDFjYmNlYTE0MGQ1NmIwYjE1YWY1ZmFhYTEzZTY4MDdhYzNiYTYxMjNkY2E2Ng==");
            request.addHeader("Cache-Control", "no-cache");
            request.setEntity(params);
            HttpResponse response = http.execute(request);

            System.out.println(MEMBER_STATS_LOG_PREFIX + " Pushed member count at " + count);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
