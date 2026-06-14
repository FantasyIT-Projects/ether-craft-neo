package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingRightClickTrigger;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamCarryEntityCapability;

public class EtherStreamDashFasterPlatingEffect extends EtherStreamDashPlatingEffect {
    public static final Identifier ID = EtherCraft.id("ether_stream_dash_faster");

    @Override
    public Identifier getId() {
        return ID;
    }

    protected double getSpeed() {
        return Config.platingEtherStreamDashFasterSpeed;
    }
}
