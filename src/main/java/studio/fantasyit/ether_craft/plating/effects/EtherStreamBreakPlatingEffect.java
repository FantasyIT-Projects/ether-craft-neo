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
import studio.fantasyit.ether_craft.stream.cap.EtherStreamBreakBlockCapability;

public class EtherStreamBreakPlatingEffect implements IPlatingEffect, IPlatingRightClickTrigger {
    public static final Identifier ID = EtherCraft.id("ether_stream_break");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, PlayerInteractEvent.RightClickItem event) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (data.isCd(level)) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingEtherStreamBreakEtherCost)) return;

        int ether = PlatingUtil.getEther(stack);
        int streamEther = Math.max(1, (int) Math.min(data.effect() * 100, ether));
        PlatingUtil.extractEther(stack, streamEther);

        Vec3 pos = entity.getEyePosition();
        Vec3 motion = entity.getLookAngle().scale(Config.platingEtherStreamBreakSpeed);
        EtherStreamEntity stream = EtherStreamEntity.create(level, streamEther, pos, motion);
        EtherStreamBreakBlockCapability cap = new EtherStreamBreakBlockCapability();
        cap.addTool(stack);
        stream.addCapability(cap);
        level.addFreshEntity(stream);

        PlatingData updated = data.copyWithCoolDown(level, Config.platingEtherStreamBreakCdTicks);
        PlatingUtil.updatePlatingData(stack, updated);
        event.setCanceled(true);
    }
}
