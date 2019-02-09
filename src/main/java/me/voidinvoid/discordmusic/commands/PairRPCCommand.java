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
        RPCSocketManager m = Radio.getInstance().getService(RPCSocketManager.class);
    }
}
