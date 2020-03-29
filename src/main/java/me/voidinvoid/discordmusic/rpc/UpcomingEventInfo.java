package me.voidinvoid.discordmusic.rpc;

public class UpcomingEventInfo {

    public boolean jingleUpcoming;
    public boolean advertUpcoming;
    public boolean pauseUpcoming;

    public UpcomingEventInfo(boolean jingleUpcoming, boolean advertUpcoming, boolean pauseUpcoming) {

        this.jingleUpcoming = jingleUpcoming;
        this.advertUpcoming = advertUpcoming;
        this.pauseUpcoming = pauseUpcoming;
    }
}
