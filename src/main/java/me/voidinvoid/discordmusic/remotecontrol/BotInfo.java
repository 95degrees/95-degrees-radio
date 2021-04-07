package me.voidinvoid.discordmusic.remotecontrol;

import me.voidinvoid.discordmusic.Radio;

import java.util.UUID;

public class BotInfo {

    public String name;
    public String instanceId;
    public String version;
    public String iconUrl;
    public String id = UUID.randomUUID().toString().substring(0, 8);

    public BotInfo(String name, String instanceId, String version, String iconUrl) {
        this.name = name;
        this.instanceId = instanceId;
        this.version = version;
        this.iconUrl = iconUrl;
    }

    public static BotInfo get() {
        var self = Radio.getInstance().getJda().getSelfUser();
        return new BotInfo(self.getName(), Radio.configName, "1.0", self.getEffectiveAvatarUrl());
    }
}
