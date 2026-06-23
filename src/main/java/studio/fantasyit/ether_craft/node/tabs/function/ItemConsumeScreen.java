package studio.fantasyit.ether_craft.node.tabs.function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;

import java.util.List;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegClient;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.function.AbstractItemConsumeFunction;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;
import studio.fantasyit.ether_craft.util.UIUtil;

public class ItemConsumeScreen extends BaseEtherNodeTabWidgetProvider<AbstractItemConsumeFunction> {

    public ItemConsumeScreen(PluginMenuContext<AbstractItemConsumeFunction> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
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
            graphics.text(font, Component.translatable("ether_craft.gui.node.item_consume.line_1", plugin.remainBurnTicks / 20), x, ly(19), 0xFFFFFFFF);
        } else {
            graphics.text(font, Component.translatable("ether_craft.gui.node.item_consume.line_idle"), x, ly(19), 0xFFFFFFFF);
        }
        graphics.text(font, Component.translatable("ether_craft.gui.node.item_consume.line_2",
                screen.getMenu().entity.getEther(), screen.getMenu().entity.getMaxEther()), x, ly(30), 0xFFFFFFFF);
        ItemStack fuelItem = plugin.container.getItem(0);
        if (!fuelItem.isEmpty()) {
            graphics.text(font, Component.translatable("ether_craft.gui.node.item_consume.line_3", fuelItem.getHoverName()), x, ly(41), 0xFFFFFFFF);
        }
    }

    @Override
    public void createWidget() {
        super.createWidget();
        collectImageAsset(EtherAdaptNodeAsset.ETHER_BAR_CTR, 26, 38);
        collectTooltipArea(new Rect2i(lx(26), ly(38), EtherAdaptNodeAsset.ETHER_BAR_CTR.w, EtherAdaptNodeAsset.ETHER_BAR_CTR.h),
                () -> List.of(Component.translatable("menu.ether_craft.ether_bar_tooltip", screen.getMenu().entity.getEther()))
        );
        FilterGuiRegClient.widget(screen, ()->plugin.filter.whitelist, plugin.installedId);
    }
}
