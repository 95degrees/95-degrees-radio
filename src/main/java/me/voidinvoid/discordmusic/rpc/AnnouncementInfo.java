package me.voidinvoid.discordmusic.rpc;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class AnnouncementInfo {

    public String title;
    public String message;
    public int stayTime;

    public AnnouncementInfo(String title, String message) {

        this.title = title;
        this.message = message;
    }

    public AnnouncementInfo setStayTime(int stayTime) {
        this.stayTime = stayTime;
        return this;
    }
}
