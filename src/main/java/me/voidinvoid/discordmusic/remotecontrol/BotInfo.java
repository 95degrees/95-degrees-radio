package me.voidinvoid.discordmusic.remotecontrol;

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
}
