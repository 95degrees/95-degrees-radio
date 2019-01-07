package me.voidinvoid.discordmusic.songs.local;

import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import me.voidinvoid.discordmusic.songs.RadioPlaylistProperties;
import me.voidinvoid.discordmusic.songs.SongType;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public class LocalRadioPlaylist extends RadioPlaylist {

    private RadioPlaylistProperties properties = new RadioPlaylistProperties();

    public LocalRadioPlaylist(Path path) {
        super(path.getFileName().toString()); //playlist dir name

        Properties prop = new Properties();

        try {

            InputStream input = new FileInputStream(path.resolve("playlist-info.txt").toString());
            prop.load(input);

            properties.setDisplayName(prop.getProperty("name", path.getFileName().toString()));
            properties.setDefault(Boolean.parseBoolean(prop.getProperty("default", "false")));
            properties.setShuffleSongs(Boolean.parseBoolean(prop.getProperty("shuffle", "true")));
            properties.setJinglesEnabled(Boolean.parseBoolean(prop.getProperty("use-jingles", "true")));
            properties.setStatusOverrideMessage(prop.getProperty("discord-status", null));
            properties.setTestingMode(Boolean.parseBoolean(prop.getProperty("testing", "false")));
            properties.setDirectMessageNotifications(Boolean.parseBoolean(prop.getProperty("direct-message-notifications", "true")));
            properties.setCoinMultiplier(Double.parseDouble(prop.getProperty("coin-multiplier", "1.0")));

        } catch (Exception ex) {
            properties.setDisplayName(path.getFileName().toString());
            ex.printStackTrace();
        }

        setSongQueue(new LocalSongQueue(path.resolve("Songs"), this, SongType.SONG, properties.isShuffleSongs()));
        setJingleQueue(new LocalSongQueue(path.resolve("Jingles"), this, SongType.JINGLE, true));

        //MIGRATION CODE
        /*
        awaitLoad();

        System.out.println("**Migrating " + path);

        dbman.getCollection("playlists").insertOne(
                new Document("_id", this.internal)
                        .append("properties", new Document("default", isDefault).append("shuffle", shuffleSongs).append("jinglesEnabled", jinglesEnabled).append("statusOverrideMessage", statusOverrideMessage).append("testing", testingMode).append("directMessageNotifications", directMessageNotifications).append("coinMultiplier", coinMultiplier))
                        .append("listing",
                                new Document("songs", songs.getQueue().stream().map(s ->
                                        {
                                            FileSong fs = ((FileSong) s);
                                            String albumArt = null;
                                            if (s.getAlbumArtFile() != null) {
                                                albumArt = UUID.randomUUID() + "";
                                                String loc = "/projects/radio/AlbumArt/Songs/" + albumArt + ".png";
                                                try {
                                                    Files.copy(s.getAlbumArtFile(), Paths.get(loc));
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                            String title = null;
                                            String artist = null;

                                            try {
                                                Mp3File m = new Mp3File(s.getFullLocation());
                                                if (m.hasId3v2Tag()) {
                                                    ID3v2 tag = m.getId3v2Tag();
                                                    title = tag.getTitle();
                                                    artist = tag.getArtist();
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            return new Document("type", "SONG")
                                                    .append("title", title)
                                                    .append("artist", artist)
                                                    .append("mbId", null)
                                                    .append("source", "PLZ FIND")
                                                    .append("albumArt", albumArt);
                                        }
                                ).collect(Collectors.toList())
                                ).append("jingles", jingles.getQueue().stream().map(s ->
                                        new Document("type", "SONG")
                                                .append("title", null)
                                                .append("artist", null)
                                                .append("mbId", null)
                                                .append("source", s.getFullLocation())
                                                .append("albumArt", null)
                                ).collect(Collectors.toList())))
        );
        */
    }

    @Override
    public RadioPlaylistProperties getProperties() {
        return properties;
    }
}
