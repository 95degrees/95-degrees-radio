package me.voidinvoid.advertisements;

import me.voidinvoid.utils.Colors;
import net.dv8tion.jda.core.EmbedBuilder;

public class Advertisement {

    private String fileName;
    private String title;
    private String url;
    private String description;
    private int colour;
    private String authorIconUrl;
    private String iconUrl;
    private String imageUrl;
    private boolean partnerAd;

    public String getFileName() {
        return fileName;
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

    public EmbedBuilder constructAdvertMessage() {
        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(title, url, authorIconUrl)
                .setDescription(description)
                .setFooter(partnerAd ? "95 Degrees Partner Advertisement" : "95 Degrees Advertisement", "https://cdn.discordapp.com/icons/202600401281744896/40d9b8c72e0a288f8f3f5c99ce1691ca.webp");

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
