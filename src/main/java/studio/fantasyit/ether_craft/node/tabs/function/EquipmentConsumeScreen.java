package studio.fantasyit.ether_craft.node.tabs.function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionEquipmentConsumeGenerator;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;
import studio.fantasyit.ether_craft.util.UIUtil;

public class EquipmentConsumeScreen extends BaseEtherNodeTabWidgetProvider<FunctionEquipmentConsumeGenerator> {

    public EquipmentConsumeScreen(PluginMenuContext<FunctionEquipmentConsumeGenerator> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
        collectImageAsset(EtherAdaptNodeAsset.ETHER_BAR_CTR, 26, 38);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        UIUtil.nineSliced(graphics, EtherAdaptNodeAsset.INFO_PANEL, lx(93), ly(15), 75, 48, 1);
        if (screen.getMenu().entity != null)
            UIUtil.renderEtherBarProgress(
                    screen.getMenu().entity.getEther(),
                    screen.getMenu().entity.getMaxEther(),
                    lx(27), ly(39), EtherAdaptNodeAsset.ETHER_BAR_CTR.w - 2, 2, graphics
            );
        Font font = Minecraft.getInstance().font;
        int x = lx(93) + 4;
        if (plugin.remainBurnTicks > 0) {
            ItemStack equipItem = plugin.container.getItem(0);
            if (!equipItem.isEmpty()) {
                graphics.text(font, Component.translatable("ether_craft.gui.node.equipment_consume.line_1", equipItem.getHoverName()), x, ly(19), 0xFFFFFFFF);
            }
            graphics.text(font, Component.translatable("ether_craft.gui.node.equipment_consume.line_2", plugin.remainBurnTicks), x, ly(30), 0xFFFFFFFF);
        } else {
            graphics.text(font, Component.translatable("ether_craft.gui.node.equipment_consume.line_idle"), x, ly(19), 0xFFFFFFFF);
        }
        graphics.text(font, Component.translatable("ether_craft.gui.node.equipment_consume.line_3",
                screen.getMenu().entity.getEther(), screen.getMenu().entity.getMaxEther()), x, ly(41), 0xFFFFFFFF);
    }
}
