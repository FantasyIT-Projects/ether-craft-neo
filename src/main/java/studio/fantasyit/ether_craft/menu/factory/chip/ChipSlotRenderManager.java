package studio.fantasyit.ether_craft.menu.factory.chip;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.item.CraftTableChipItem;
import studio.fantasyit.ether_craft.item.ProcessChipItem;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryAsset;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * 芯片槽位渲染管理器：维护「物品谓词 -&gt; 渲染器」的注册表。
 *
 * <p>只负责"某个物品该用哪个渲染器画"，不关心"哪些槽位参与"——后者由界面决定。
 * 按注册顺序取首个命中的谓词；未命中时调用方应回落到原版槽位渲染，保证未注册芯片零行为变化。
 *
 * <p>本类为客户端专用（引用 {@code GuiGraphicsExtractor}），不要在任何服务端代码路径中引用。
 */
public final class ChipSlotRenderManager {
    private static final List<ChipSlotRenderer> ENTRIES = new ArrayList<>();

    private ChipSlotRenderManager() {
    }

    public static void register(@NotNull ChipSlotRenderer renderer) {
        ENTRIES.add(renderer);
    }

    public static void collect() {
        ENTRIES.clear();
        register(new ChannelChipRenderer(EtherProcessFactoryAsset.CHIP_CONCAT, true, ProcessChipItem.CONCAT));
        register(new ChannelChipRenderer(EtherProcessFactoryAsset.CHIP_CRAFTING, false, CraftTableChipItem.CHIP_ID));
        register(new ChannelChipRenderer(EtherProcessFactoryAsset.CHIP_TRANSMUTING, false, ProcessChipItem.TRANSMUTING));
    }

    public static @Nullable ChipSlotRenderer find(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (ChipSlotRenderer entry : ENTRIES) {
            if (entry.predicate(stack)) return entry;
        }
        return null;
    }

    public static boolean hasRenderer(ItemStack stack) {
        return find(stack) != null;
    }

    public static boolean isHoveringItemArea(float mouseX, float mouseY, int screenX, int screenY) {
        return mouseX >= screenX && mouseX < screenX + 16
                && mouseY >= screenY && mouseY < screenY + 16;
    }

    /**
     * 命中则用对应渲染器绘制并返回 true；未命中返回 false，调用方应回落到原版槽位渲染。
     */
    public static boolean render(GuiGraphicsExtractor graphics, EtherProcessFactoryContainerMenu menu,
                                 Slot slot, ItemStack stack, int mouseX, int mouseY, int leftPos, int topPos) {
        ChipSlotRenderer renderer = find(stack);
        if (renderer == null) return false;
        renderer.render(graphics, menu, mouseX, mouseY, slot.x, slot.y, leftPos + slot.x, topPos + slot.y, stack);
        return true;
    }
}
