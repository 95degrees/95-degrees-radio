package me.voidinvoid.discordmusic.utils;

import me.voidinvoid.discordmusic.Radio;
import net.dv8tion.jda.api.entities.Emote;

/**
 * Guardian - 29/07/2019
 * This code was developed by VoidInVoid / Exfusion
 * © 2019
 */

public final class Emoji {

    public static void init() {
        var g = Radio.getInstance().getJda().getGuildById("605044866912747521");

        if (g == null) {
            throw new RuntimeException("Emoji guild is null!!");
        }

        TICK_EMOTE = g.getEmoteById("605195867875442748");
        WARN_EMOTE = g.getEmoteById("605195885772537858");
        QUESTION_EMOTE = g.getEmoteById("605197720915017728");
        CROSS_EMOTE = g.getEmoteById("605195856160882698");
        GUARDIAN_EMOTE = g.getEmoteById("605193685952495622");
        RADIO_EMOTE = g.getEmoteById("605045615960784896");
        DEGREECOIN_EMOTE = g.getEmoteById("605875991172218894");
        JOIN_EMOTE = g.getEmoteById("605203966183211032");
        LEAVE_EMOTE = g.getEmoteById("605203966778933268");
        REDDIT_EMOTE = g.getEmoteById("623196197179359232");
        GNOME_EMOTE = g.getEmoteById("628597995830509589");
        LOADING_EMOTE = g.getEmoteById("624223121783652363");

        TICK = TICK_EMOTE.getAsMention();
        WARN = WARN_EMOTE.getAsMention();
        QUESTION = QUESTION_EMOTE.getAsMention();
        CROSS = CROSS_EMOTE.getAsMention();
        GUARDIAN = GUARDIAN_EMOTE.getAsMention();
        RADIO = RADIO_EMOTE.getAsMention();
        DEGREECOIN = DEGREECOIN_EMOTE.getAsMention();
        JOIN = JOIN_EMOTE.getAsMention();
        LEAVE = LEAVE_EMOTE.getAsMention();
        REDDIT = REDDIT_EMOTE.getAsMention();
        GNOME = GNOME_EMOTE.getAsMention();
        LOADING = LOADING_EMOTE.getAsMention();
    }

    public static Emote TICK_EMOTE;
    public static Emote WARN_EMOTE;
    public static Emote QUESTION_EMOTE;
    public static Emote CROSS_EMOTE;
    public static Emote GUARDIAN_EMOTE;
    public static Emote RADIO_EMOTE;
    public static Emote DEGREECOIN_EMOTE;
    public static Emote JOIN_EMOTE;
    public static Emote LEAVE_EMOTE;
    public static Emote REDDIT_EMOTE;
    public static Emote GNOME_EMOTE;
    public static Emote LOADING_EMOTE;

    public static String TICK;
    public static String WARN;
    public static String QUESTION;
    public static String CROSS;
    public static String GUARDIAN;
    public static String RADIO;
    public static String DEGREECOIN;
    public static String JOIN;
    public static String LEAVE;
    public static String REDDIT;
    public static String GNOME;
    public static String LOADING;
}
