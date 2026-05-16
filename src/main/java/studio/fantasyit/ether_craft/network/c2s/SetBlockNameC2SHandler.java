package studio.fantasyit.ether_craft.network.c2s;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.network.s2c.SyncBlockNameS2C;

public class SetBlockNameC2SHandler {
    public static void handle(SetBlockNameC2S message, Player player) {
        var be = player.level().getBlockEntity(message.pos());
        if (be instanceof EtherAdaptNodeEntity node) {
            node.name = message.name();
            node.setChanged();
        } else if (be instanceof EtherProcessFactoryEntity factory) {
            factory.name = message.name();
            factory.setChanged();
        }
        if (player.level() instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersInDimension(serverLevel,
                    new SyncBlockNameS2C(message.pos(), message.name()));
        }
    }
}
