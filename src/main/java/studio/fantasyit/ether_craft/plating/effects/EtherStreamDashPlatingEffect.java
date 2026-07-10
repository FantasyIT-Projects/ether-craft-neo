package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingKeyTrigger;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingRightClickTrigger;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamCarryEntityCapability;

public class EtherStreamDashPlatingEffect implements IPlatingEffect, IPlatingRightClickTrigger, IPlatingKeyTrigger {
    public static final Identifier ID = EtherCraft.id("ether_stream_dash");

    @Override
    public Identifier getId() {
        return ID;
    }

    protected double getSpeed(PlatingData platingData) {
        return Config.platingEtherStreamDashSpeed;
    }

    protected int getEtherContains(PlatingData platingData) {
        return (int) platingData.effect();
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, PlayerInteractEvent.RightClickItem event) {
        if (work(data, stack, entity)) return;
        event.setCanceled(true);
    }

    private boolean work(PlatingData data, ItemStack stack, LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return true;
        if (data.isCd(level)) return true;
        if (!PlatingUtil.canExtractEther(stack, Config.platingEtherStreamDashEtherCost)) return true;
        if (entity.hasData(AttachmentDataRegistry.TAKEN_BY_ETHER_STREAM) && entity.getData(AttachmentDataRegistry.TAKEN_BY_ETHER_STREAM))
            return true;
        PlatingUtil.extractEtherWithEntityContext(entity, stack, Config.platingEtherStreamDashEtherCost);
        int streamEther = Math.max(1, getEtherContains(data));

        Vec3 pos = entity.getEyePosition();
        Vec3 motion = entity.getLookAngle().scale(getSpeed(data));
        EtherStreamEntity stream = EtherStreamEntity.create(level, streamEther, pos, motion);
        stream.setRealCanReceiveEther(Config.platingEtherStreamDashEtherCost);
        stream.setHitExclude(entity);
        EtherStreamCarryEntityCapability etherStreamCarryEntityCapability = new EtherStreamCarryEntityCapability(entity.blockPosition());
        etherStreamCarryEntityCapability.forceTakeEntity(stream, entity);
        stream.addCapability(etherStreamCarryEntityCapability);
        level.addFreshEntity(stream);

        PlatingData updated = data.copyWithCoolDown(level, Config.platingEtherStreamDashCdTicks);
        PlatingUtil.updatePlatingData(stack, updated);
        return false;
    }

    @Override
    public void onKeyTrigger(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, EquipmentSlot slot) {
        work(data, stack, entity);
    }
}
