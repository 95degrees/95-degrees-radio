package me.voidinvoid.discordmusic.songs.database;

import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.albumart.AlbumArt;
import me.voidinvoid.discordmusic.songs.albumart.RemoteAlbumArt;
import me.voidinvoid.discordmusic.songs.database.triggers.SongTrigger;
import me.voidinvoid.discordmusic.songs.database.triggers.TriggerActivation;
import me.voidinvoid.discordmusic.songs.database.triggers.TriggerType;
import me.voidinvoid.discordmusic.tasks.Parameter;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseSong extends Song {

    private String title;
    private String artist;
    private String mbId;
    private String source;
    private String albumArtId;
    private AlbumArt albumArt;
    private List<SongTrigger> triggers = new ArrayList<>();
    private String sourceName;

    public DatabaseSong(SongType songType, Document document, String sourceName) {
        this(songType, document);
        this.sourceName = sourceName;
    }

    @SuppressWarnings("unchecked")
    public DatabaseSong(SongType songType, Document document) {
        super(songType);

        title = document.getString("title");
        artist = document.getString("artist");
        mbId = document.getString("mbId");
        source = document.getString("source");
        albumArtId = document.getString("albumArt");

        if (albumArtId != null) {
            albumArt = new RemoteAlbumArt("https://void.in.net/radio/albumart/" + albumArtId);
            //albumArt = new LocalAlbumArt(Paths.get("/home/discord/radio_old/AlbumArt/Songs/" + albumArtId + ".png"));
        }

        if (document.containsKey("triggerActions")) {
            List<Document> actions = (List<Document>) document.get("triggerActions");
            for (Document d : actions) {
                try {
                    TriggerType type = TriggerType.valueOf(d.getString("type"));
                    TriggerActivation on = d.containsKey("on") ? TriggerActivation.valueOf(d.getString("on")) : TriggerActivation.SONG_START;
                    Document params = d.containsKey("params") ? d.get("params", Document.class) : null;
                    Map<Parameter, Object> pm = new HashMap<>();

                    if (params != null) {
                        params.forEach((key, value) -> {
                            Parameter p = Parameter.of(key);
                            pm.put(p, value);
                        });
                    }

                    ParameterList paramList = new ParameterList(pm);
                    triggers.add(new SongTrigger(type, on, paramList));
                } catch (Exception e) {
                    System.out.println("Warning: invalid trigger for song " + title);
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getArtist() {
        return artist;
    }

    public String getMbId() {
        return mbId;
    }

    public List<SongTrigger> getTriggers() {
        return triggers;
    }

    public String getSourceName() {
        return sourceName;
    }

    @Override
    public String getInternalName() {
        return albumArtId;
    }

    @Override
    public String getLavaIdentifier() {
        return source;
    }

    @Override
    public AlbumArt getAlbumArt() {
        var p = getType().getAlbumArt(this);

        //if this song type overrides album art, use that. otherwise, use our own album art
        return p == null ? albumArt : p;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}
