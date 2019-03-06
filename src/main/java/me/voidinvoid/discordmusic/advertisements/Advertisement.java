package me.voidinvoid.discordmusic.advertisements;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.songs.NetworkSong;
import me.voidinvoid.discordmusic.songs.Song;
import me.voidinvoid.discordmusic.songs.SongType;
import me.voidinvoid.discordmusic.utils.Colors;
import net.dv8tion.jda.core.EmbedBuilder;

public class Advertisement {

    private String identifier;
    private String title;
    private String url;
    private String description;
    private int colour;
    private String authorIconUrl;
    private String iconUrl;
    private String imageUrl;
    private boolean partnerAd;
    private NetworkSong song;

    public String getIdentifier() {
        return identifier;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public int getColour() {
        return colour;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public Song getSong() {
        return song;
    }

    public void generateSong() {
        Radio.getInstance().getOrchestrator().createNetworkTrack(SongType.ADVERTISEMENT, identifier, n -> {
            song = n;
        });
    }

    public EmbedBuilder constructAdvertMessage() {
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(title, url, authorIconUrl)
                .setDescription(description)
                .setFooter(partnerAd ? "95 Degrees Partner Advertisement" : "95 Degrees Advertisement", "https://cdn.discordapp.com/icons/202600401281744896/06cd2f1e234f8b757c00a20f9a58e345.webp");

        if (imageUrl != null) embed.setImage(imageUrl);
        if (iconUrl != null) embed.setThumbnail(iconUrl);
        if (colour == 0) {
            embed.setColor(Colors.ACCENT_ADVERTISEMENT);
        } else {
            embed.setColor(colour);
        }

        return embed;
    }
}
