package me.voidinvoid;

public final class Utils {

    public static String getFormattedMsTime(long time) {
        long second = (time / 1000) % 60;
        long minute = (time / (1000 * 60)) % 60;
        long hours = (time / (1000 * 60 * 60)) % 60;

        return (hours > 0 ? String.format("%02d:", hours) : "") + String.format("%02d:%02d", minute, second);
    }

    public static String getFormattedMsTimeLabelled(long time) {
        long seconds = (time / 1000) % 60;
        long minutes = (time / (1000 * 60)) % 60;
        long hours = (time / (1000 * 60 * 60)) % 60;

        String format = "";

        if (hours > 0) {
            format += hours + "h ";
        }

        if (hours == 0 || minutes > 0) {
            format += minutes + "m ";
        }

        if (minutes == 0 || seconds > 0) {
            format += seconds + "s";
        }

        //if (minutes > 0) return minute + "m" + ((second == 0) ? "" : " " + second + "s");
        return format.trim();
    }

    public static String escape(String text) {
        if (text == null) return "";
        if (text.startsWith("http://") || text.startsWith("https://")) return text; //todo HACK
        return text.replace("_", "\\_").replace("*", "\\*").replace("~", "\\~").replace("`", "\\`");
    }
}
