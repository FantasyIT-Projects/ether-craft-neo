package studio.fantasyit.ether_craft.integration.jade;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.data.ProgressingPlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import java.util.List;

public enum PlatingItemEntityProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final Identifier UID = EtherCraft.id("plating_item_entity");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof ItemEntity itemEntity)) return;
        ItemStack stack = itemEntity.getItem();

        if (PlatingUtil.isPlatingInProgress(stack)) {
            addInProgressTooltip(tooltip, stack);
        } else if (PlatingUtil.hasPlating(stack)) {
            addCompletedTooltip(tooltip, stack);
        }
    }

    private static void addInProgressTooltip(ITooltip tooltip, ItemStack stack) {
        List<ProgressingPlatingData> inProgress = PlatingUtil.getInProgress(stack);
        int ether = PlatingUtil.getEther(stack);
        long startTime = stack.getOrDefault(DataComponentRegistry.PLATING_START_TIME, 0L);

        tooltip.add(Component.translatable("tooltip.ether_craft.plating.in_progress"));

        for (ProgressingPlatingData p : inProgress) {
            Identifier id = p.id();
            tooltip.add(Component.literal("  ").append(
                    Component.translatable("tooltip.ether_craft.plating.effect." + id.toLanguageKey())
            ));
        }

        tooltip.add(Component.translatable("tooltip.ether_craft.plating.ether", ether));

        if (Minecraft.getInstance().level != null) {
            long elapsed = Minecraft.getInstance().level.getGameTime() - startTime;
            long remaining = Math.max(0, Config.platingDurationTicks - elapsed);
            double seconds = remaining / 20.0;
            tooltip.add(Component.translatable("tooltip.ether_craft.plating.remaining", String.format("%.1f", seconds)));
        }
    }

    private static void addCompletedTooltip(ITooltip tooltip, ItemStack stack) {
        List<PlatingData> data = PlatingUtil.getPlatingData(stack);
        int ether = PlatingUtil.getEther(stack);

        tooltip.add(Component.translatable("tooltip.ether_craft.plating.title"));

        for (PlatingData d : data) {
            tooltip.add(Component.literal("  ").append(
                    Component.translatable("tooltip.ether_craft.plating.effect." + d.id().toLanguageKey(),
                            String.format("%.2f", d.effect()))
            ));
        }

        tooltip.add(Component.translatable("tooltip.ether_craft.plating.ether", ether));
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
