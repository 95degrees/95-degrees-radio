package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.utils.ChannelScope;

public class AdvertisementCommand extends Command {

    AdvertisementCommand() {
        super("ad", "Runs an advertisement in the text channel", null, ChannelScope.DJ_CHAT, "advert", "advertisement");
    }

    @Override
    public void invoke(CommandData data) {
        if (Radio.instance.getAdvertisementManager() == null) {
            data.error("Adverts are not currently enabled");
            return;
        }

        Radio.instance.getAdvertisementManager().pushAdvertisement();
        data.success("Queued an advert");
    }
}
