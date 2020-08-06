package me.voidinvoid.discordmusic.utils;

import me.voidinvoid.discordmusic.Radio;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.file.Files;
import java.util.Set;

/**
 * Guardian - 29/07/2019
 * This code was developed by VoidInVoid / Exfusion
 * © 2019
 */

public final class Emoji {

    public static Emoji TICK;
    public static Emoji WARN;
    public static Emoji QUESTION;
    public static Emoji CROSS;
    public static Emoji GUARDIAN;
    public static Emoji RADIO;
    public static Emoji DEGREECOIN;
    public static Emoji JOIN;
    public static Emoji LEAVE;
    public static Emoji REDDIT;
    public static Emoji COUNTDOWN_CLOCK;
    public static Emoji COUNTDOWN_0;
    public static Emoji COUNTDOWN_1;
    public static Emoji COUNTDOWN_2;
    public static Emoji COUNTDOWN_3;
    public static Emoji COUNTDOWN_4;
    public static Emoji COUNTDOWN_5;
    public static Emoji COUNTDOWN_6;
    public static Emoji COUNTDOWN_7;
    public static Emoji COUNTDOWN_8;
    public static Emoji COUNTDOWN_9;
    public static Emoji COUNTDOWN_25;
    public static Emoji COUNTDOWN_50;
    public static Emoji COUNTDOWN_75;
    public static Emoji COUNTDOWN_100;
    public static Emoji GNOME;
    public static Emoji LOADING;
    public static Emoji DIVIDER;
    public static Emoji DIVIDER_SMALL;

    public static Emoji PLAY;
    public static Emoji PAUSE;

    public static Emoji SEEK_BAR_LEFT_BORDER;
    public static Emoji SEEK_BAR_RIGHT_BORDER;
    public static Emoji SEEK_BAR_MID_INCOMPLETE;
    public static Emoji SEEK_BAR_MID_25;
    public static Emoji SEEK_BAR_MID_50;
    public static Emoji SEEK_BAR_MID_75;
    public static Emoji SEEK_BAR_MID_100;

    public static void init() {
        var g = Radio.getInstance().getJda().getGuildById("605044866912747521");

        if (g == null) {
            throw new RuntimeException("Emoji guild is null!!");
        }

        TICK = new Emoji(g.getEmoteById("605195867875442748"));
        WARN = new Emoji(g.getEmoteById("605195885772537858"));
        QUESTION = new Emoji(g.getEmoteById("605197720915017728"));
        CROSS = new Emoji(g.getEmoteById("605195856160882698"));
        GUARDIAN = new Emoji(g.getEmoteById("605193685952495622"));
        RADIO = new Emoji(g.getEmoteById("605045615960784896"));
        DEGREECOIN = new Emoji(g.getEmoteById("605875991172218894"));
        JOIN = new Emoji(g.getEmoteById("605203966183211032"));
        LEAVE = new Emoji(g.getEmoteById("605203966778933268"));
        REDDIT = new Emoji(g.getEmoteById("623196197179359232"));
        COUNTDOWN_CLOCK = new Emoji(g.getEmoteById("613772892588736543"));
        COUNTDOWN_0 = new Emoji(g.getEmoteById("613772893561552896"));
        COUNTDOWN_1 = new Emoji(g.getEmoteById("613772893578330123"));
        COUNTDOWN_2 = new Emoji(g.getEmoteById("613772893909680136"));
        COUNTDOWN_3 = new Emoji(g.getEmoteById("613772894211801125"));
        COUNTDOWN_4 = new Emoji(g.getEmoteById("613772894505271296"));
        COUNTDOWN_5 = new Emoji(g.getEmoteById("613772894656397318"));
        COUNTDOWN_6 = new Emoji(g.getEmoteById("613772896044843051"));
        COUNTDOWN_7 = new Emoji(g.getEmoteById("613772897177042984"));
        COUNTDOWN_8 = new Emoji(g.getEmoteById("613772898829729812"));
        COUNTDOWN_9 = new Emoji(g.getEmoteById("613772898460631052"));
        COUNTDOWN_25 = new Emoji(g.getEmoteById("613772898473213963"));
        COUNTDOWN_50 = new Emoji(g.getEmoteById("613772898464694325"));
        COUNTDOWN_75 = new Emoji(g.getEmoteById("613772898896969739"));
        COUNTDOWN_100 = new Emoji(g.getEmoteById("613772898464694321"));
        GNOME = new Emoji(g.getEmoteById("628597995830509589"));
        LOADING = new Emoji(g.getEmoteById("624223121783652363"));
        DIVIDER = new Emoji(g.getEmoteById("710918643029901333"));
        DIVIDER_SMALL = new Emoji(g.getEmoteById("711631476718043136"));

        PLAY = new Emoji(g.getEmoteById("717119119572598857"));
        PAUSE = new Emoji(g.getEmoteById("717119119484256307"));

        SEEK_BAR_LEFT_BORDER = new Emoji(g.getEmoteById("718559656078868570"));
        SEEK_BAR_RIGHT_BORDER = new Emoji(g.getEmoteById("718559452642410586"));
        SEEK_BAR_MID_INCOMPLETE = new Emoji(g.getEmoteById("718559452114190398"));
        SEEK_BAR_MID_25 = new Emoji(g.getEmoteById("718559452147482685"));
        SEEK_BAR_MID_50 = new Emoji(g.getEmoteById("718559452105801769"));
        SEEK_BAR_MID_75 = new Emoji(g.getEmoteById("718559452369911818"));
        SEEK_BAR_MID_100 = new Emoji(g.getEmoteById("718559452478832730"));
    }

    private final Emote emote;

    private Emoji(Emote emote) {
        this.emote = emote;
    }

    public Emote getEmote() {
        return emote;
    }

    @Override
    public String toString() {
        return emote.getAsMention();
    }

    public static Emoji getOrCreateUserEmoji(User user, Set<String> requiredIds) {
        var guild = Radio.getInstance().getJda().getGuildById("734895274043899944"); //user icon emoji server

        if (guild == null) {
            return null;
        }

        var emotes = guild.getEmotesByName(user.getId(), true);

        if (!emotes.isEmpty()) {
            return new Emoji(emotes.get(0));
        }

        if (guild.getEmotes().size() >= guild.getMaxEmotes()) { //we need to delete some, but make sure we dont delete any from the required ids set
            var toRemove = guild.getEmotes().stream().filter(e -> requiredIds != null && !requiredIds.contains(e.getName())).findAny().orElse(null);

            if (toRemove != null) {
                System.out.println("Deleting " + toRemove + " emoji to make space!");
                toRemove.delete().queue();
            } else {
                System.out.println("Couldn't make any space for emoji!");
                return null;
            }
        }

        var image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        var gfx = image.createGraphics();

        try {
            var url = new URL(user.getEffectiveAvatarUrl() + "?size=64");
            var avatar = ImageIO.read(url);

            var circle = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

            var g2 = circle.createGraphics();
            g2.drawImage(avatar, 0, 0, 64, 64, null);

            gfx.drawImage(createCircleCrop(avatar), 0, 0, 64, 64, null);

            gfx.dispose();

            var f = Files.createTempFile("avatar", ".png").toFile();
            ImageIO.write(image, "png", f);

            var emote = guild.createEmote(user.getId(), Icon.from(f)).complete();

            if (emote != null) {
                return new Emoji(emote);
            }

        } catch (Exception ex) {
            System.out.println("Error downloading user avatar!");
            ex.printStackTrace();
        }

        return null;
    }

    private static BufferedImage createCircleCrop(BufferedImage image) {
        var out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var gfx = out.createGraphics();

        gfx.setComposite(AlphaComposite.Src);
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gfx.setColor(Color.WHITE);
        gfx.fill(new Ellipse2D.Double(0, 0, image.getWidth(), image.getHeight()));

        gfx.setComposite(AlphaComposite.SrcAtop);
        gfx.drawImage(image, 0, 0, null);

        gfx.dispose();

        return out;
    }
}

