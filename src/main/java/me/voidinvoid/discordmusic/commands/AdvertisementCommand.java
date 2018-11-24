package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.advertisements.Advertisement;
import me.voidinvoid.discordmusic.utils.ChannelScope;

import java.util.List;
import java.util.stream.Collectors;

public class AdvertisementCommand extends Command {

    AdvertisementCommand() {
        super("ad", "Runs an advertisement in the text channel", "[advert file name ...]", ChannelScope.DJ_CHAT, "advert", "advertisement");
    }

    @Override
    public void invoke(CommandData data) {
        if (Radio.instance.getAdvertisementManager() == null) {
            data.error("Adverts are not currently enabled");
            return;
        }

        if (data.getArgs().length > 0) {
            List<Advertisement> ads = Radio.instance.getAdvertisementManager().getAdverts();

            String title = data.getArgsString();

            Advertisement ad = ads.stream().filter(a -> title.equals(a.getFileName())).findFirst().orElse(null);

            if (ad == null) {
                data.error("Invalid ad file name. Valid file names:\n" + ads.stream().map(Advertisement::getFileName).collect(Collectors.joining("\n", "`", "`")));
                return;
            }
        }

        Radio.instance.getAdvertisementManager().pushAdvertisement();
        data.success("Queued an advert");
    }
}
