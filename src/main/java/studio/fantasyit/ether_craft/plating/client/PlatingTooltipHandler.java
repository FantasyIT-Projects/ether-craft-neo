package studio.fantasyit.ether_craft.plating.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.data.ProgressingPlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import java.util.List;

@EventBusSubscriber(modid = EtherCraft.MODID, value = Dist.CLIENT)
public class PlatingTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        if (PlatingUtil.isPlatingInProgress(stack)) {
            addInProgressTooltip(stack, tooltip);
        } else if (PlatingUtil.hasPlating(stack)) {
            addCompletedTooltip(stack, tooltip);
        }
    }

    private static void addInProgressTooltip(ItemStack stack, List<Component> tooltip) {
        List<ProgressingPlatingData> inProgress = PlatingUtil.getInProgress(stack);
        int ether = PlatingUtil.getEther(stack);
        long startTime = stack.getOrDefault(DataComponentRegistry.PLATING_START_TIME, 0L);

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.ether_craft.plating.in_progress")
                .withStyle(net.minecraft.ChatFormatting.YELLOW));

        for (ProgressingPlatingData p : inProgress) {
            Identifier id = p.id();
            tooltip.add(Component.literal("  ").append(Component.translatable("tooltip.ether_craft.plating.effect." + id.toLanguageKey()))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        tooltip.add(Component.translatable("tooltip.ether_craft.plating.ether", ether)
                .withStyle(net.minecraft.ChatFormatting.AQUA));

        if (Minecraft.getInstance().level != null) {
            long elapsed = Minecraft.getInstance().level.getGameTime() - startTime;
            long remaining = Math.max(0, Config.platingDurationTicks - elapsed);
            double seconds = remaining / 20.0;
            tooltip.add(Component.translatable("tooltip.ether_craft.plating.remaining", String.format("%.1f", seconds))
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    private static void addCompletedTooltip(ItemStack stack, List<Component> tooltip) {
        List<PlatingData> data = PlatingUtil.getPlatingData(stack);
        int ether = PlatingUtil.getEther(stack);

        if (data.isEmpty()) return;

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.ether_craft.plating.title")
                .withStyle(net.minecraft.ChatFormatting.GOLD));

        for (PlatingData d : data) {
            tooltip.add(Component.literal("  ").append(
                    Component.translatable("tooltip.ether_craft.plating.effect." + d.id().toLanguageKey(),
                            String.format("%.2f", d.effect()))
            ).withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        tooltip.add(Component.translatable("tooltip.ether_craft.plating.ether", ether)
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}