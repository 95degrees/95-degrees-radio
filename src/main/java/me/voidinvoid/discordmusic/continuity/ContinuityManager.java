package me.voidinvoid.discordmusic.continuity;

import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.SongOrchestrator;
import me.voidinvoid.discordmusic.songs.RadioPlaylist;
import org.bson.Document;

import java.util.stream.Collectors;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
@Deprecated //NOT IMPLEMENTED YET
public class ContinuityManager implements RadioService { //TODO

    public ContinuityManager() {

    }

    @Override
    public void onShutdown() {
        SongOrchestrator orch = Radio.getInstance().getOrchestrator();
        DatabaseManager db = Radio.getInstance().getService(DatabaseManager.class);

        if (db == null) return;
        Document state = new Document();

        if (orch.getActivePlaylist() instanceof RadioPlaylist) {
            state.put("playlist", orch.getActivePlaylist().getInternal());

            state.put("current_song", orch.getCurrentSong());

            RadioPlaylist p = (RadioPlaylist) orch.getActivePlaylist();
            state.put("queue", p.getSongs().getSuggestions().stream().map(ContinuitySuggestionData::new).collect(Collectors.toList()));
        }

        db.getCollection("internal");
    }
}
