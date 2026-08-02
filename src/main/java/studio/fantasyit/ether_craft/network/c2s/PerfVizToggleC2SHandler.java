package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.world.entity.player.Player;
import studio.fantasyit.ether_craft.perf.ServerPerf;

public class PerfVizToggleC2SHandler {
    public static void handle(PerfVizToggleC2S message, Player player) {
        ServerPerf.setPlayer(player, message.enabled());
    }
}
