package studio.fantasyit.ether_craft.node.tabs.function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;

import java.util.List;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionEtherConverter;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;
import studio.fantasyit.ether_craft.util.UIUtil;

public class EtherConverterScreen extends BaseEtherNodeTabWidgetProvider<FunctionEtherConverter> {

    public EtherConverterScreen(PluginMenuContext<FunctionEtherConverter> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
    }

    @Override
    public void createWidget() {
        collectImageAsset(EtherAdaptNodeAsset.ETHER_BAR_CTR, 26, 38);
        collectTooltipArea(new Rect2i(lx(26), ly(38), EtherAdaptNodeAsset.ETHER_BAR_CTR.w, EtherAdaptNodeAsset.ETHER_BAR_CTR.h),
                () -> List.of(Component.translatable("menu.ether_craft.ether_bar_tooltip", screen.getMenu().entity.getEther()))
        );
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
        int state = screen.getMenu().entity.getSyncedPluginData(plugin.installedId, FunctionEtherConverter.STATE);
        if (state == 1) {
            graphics.text(font, Component.translatable("ether_craft.gui.node.ether_converter.line_1"), x, ly(19), 0xFFFFFFFF);
        } else {
            graphics.text(font, Component.translatable("ether_craft.gui.node.ether_converter.line_idle"), x, ly(19), 0xFFFFFFFF);
        }
        graphics.text(font, Component.translatable("ether_craft.gui.node.ether_converter.line_2", Config.nodeEtherConverterCoefficient), x, ly(30), 0xFFFFFFFF);
        graphics.text(font, Component.translatable("ether_craft.gui.node.ether_converter.line_3",
                screen.getMenu().entity.getEther(), screen.getMenu().entity.getMaxEther()), x, ly(41), 0xFFFFFFFF);
    }
}
