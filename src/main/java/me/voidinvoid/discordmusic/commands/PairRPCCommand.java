package me.voidinvoid.discordmusic.commands;

import me.voidinvoid.discordmusic.Radio;
import me.voidinvoid.discordmusic.rpc.RPCSocketManager;
import me.voidinvoid.discordmusic.utils.ChannelScope;

/**
 * This code was developed by VoidInVoid / Exfusion
 * 2019
 */
public class PairRPCCommand extends Command {

    PairRPCCommand() {
        super("pair-rpc", "Pairs your Discord account with the RPC client", "<code>", ChannelScope.RADIO_AND_DJ_CHAT);
    }

    @Override
    public void invoke(CommandData data) {
        if (data.isConsole()) {
            data.error("A user is required");
            return;
        }

        if (data.getArgs().length < 1) {
            data.error("A code is required! Use the RPC client and click on the user icon in the top right to link your account");
            return;
        }

        RPCSocketManager m = Radio.getInstance().getService(RPCSocketManager.class);
        if (!m.linkAccount(data.getMember(), data.getArgs()[0])) {
            data.error("Couldn't link your account. Check that the link code is correct");
            return;
        }

        data.success("Paired account successfully!");
    }
}
