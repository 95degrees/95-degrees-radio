package me.voidinvoid.discordmusic.songs.database;

import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.songs.RadioPlaylistProperties;
import me.voidinvoid.discordmusic.songs.SongType;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Properties;

public class DatabaseRadioPlaylist extends RadioPlaylist {

    private RadioPlaylistProperties properties = new RadioPlaylistProperties();

    @SuppressWarnings("unchecked")
    public DatabaseRadioPlaylist(Document document) {
        super(document.get("_id").toString()); //playlist _id

        Properties prop = new Properties();

        Document props = document.get("properties", Document.class);

        properties.setDisplayName(props.getString("name"));
        properties.setDefault(props.getBoolean("default", false));
        properties.setShuffleSongs(props.getBoolean("shuffle", true));
        properties.setJinglesEnabled(props.getBoolean("jinglesEnabled", true));
        properties.setStatusOverrideMessage(props.getString("statusOverrideMessage"));
        properties.setTestingMode(props.getBoolean("testing", false));
        properties.setDirectMessageNotifications(props.getBoolean("directMessageNotifications", true));
        properties.setCoinMultiplier((double) props.getOrDefault("coinMultiplier", 1.0));

        Document listing = document.get("listing", Document.class);

        setSongQueue(new DatabaseSongQueue((ArrayList<Document>) listing.get("songs"), this, SongType.SONG, properties.isShuffleSongs()));
        setJingleQueue(new DatabaseSongQueue((ArrayList<Document>) listing.get("jingles"), this, SongType.JINGLE, true));
    }

    @Override
    public RadioPlaylistProperties getProperties() {
        return properties;
    }
}
