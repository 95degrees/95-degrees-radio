package me.voidinvoid.discordmusic.songs.database;

import me.voidinvoid.discordmusic.songs.AlbumArtType;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.songs.database.triggers.SongTrigger;
import me.voidinvoid.discordmusic.songs.database.triggers.TriggerActivation;
import me.voidinvoid.discordmusic.songs.database.triggers.TriggerType;
import me.voidinvoid.discordmusic.tasks.Parameter;
import me.voidinvoid.discordmusic.tasks.ParameterList;
import me.voidinvoid.discordmusic.utils.AlbumArt;
import org.bson.Document;

import java.nio.file.Path;
import java.nio.file.Paths;
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
    private Path albumArt;
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
            albumArt = Paths.get("/projects/radio/AlbumArt/Songs/" + albumArtId + ".png");
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

    public String getTitle() {
        return title;
    }

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
    public String getFriendlyName() {
        return artist + " - " + title;
    }

    @Override
    public String getFileName() {
        return "DB/" + albumArtId; //todo?
    }

    @Override
    public String getFullLocation() {
        return source;
    }

    @Override
    public AlbumArtType getAlbumArtType() {
        return AlbumArtType.FILE;
    }

    @Override
    public Path getAlbumArtFile() {
        Path p = getType().getAlbumArt(this);

        //does the song type have a specific album art? if so return that
        //otherwise does it have its own album art? if so return that
        //otherwise return the 'not found' fallback album art
        return p == null ? albumArt == null ? AlbumArt.FALLBACK_ALBUM_ART : albumArt : p;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}
