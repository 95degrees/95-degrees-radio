package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.advertisements.Advertisement;
import me.voidinvoid.discordmusic.advertisements.AdvertisementManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;

import java.util.List;
import java.util.stream.Collectors;

public class AdvertisementCommand extends Command {

    AdvertisementCommand() {
        super("ad", "Runs an advertisement in the text channel", "[advert name ...]", ChannelScope.DJ_CHAT, "advert", "advertisement");
    }

    @Override
    public void invoke(CommandData data) {
        AdvertisementManager adman = Radio.getInstance().getService(AdvertisementManager.class);

        if (adman == null) {
            data.error("Adverts are not currently enabled");
            return;
        }

        if (data.getArgs().length > 0) {
            List<Advertisement> ads = adman.getAdverts();

            String title = data.getArgsString();

            Advertisement ad = ads.stream().filter(a -> title.equals(a.getTitle())).findAny().orElse(null);

            if (ad == null) {
                data.error("Invalid ad file name. Valid file names:\n" + ads.stream().map(Advertisement::getTitle).collect(Collectors.joining("\n", "`", "`")));
                return;
            }

            Radio.getInstance().getOrchestrator().getAwaitingSpecialSongs().add(ad.getSong());
            data.success("Queued specified advert");
            return;
        }

        adman.pushAdvertisement();
        data.success("Queued an advert");
    }
}
