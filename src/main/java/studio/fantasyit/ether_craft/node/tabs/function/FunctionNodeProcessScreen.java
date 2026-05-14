package studio.fantasyit.ether_craft.node.tabs.function;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.network.c2s.SetFilterSlotC2S;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegClient;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionNodeProcess;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;
import studio.fantasyit.ether_craft.util.UIUtil;

public class FunctionNodeProcessScreen extends BaseEtherNodeTabWidgetProvider<FunctionNodeProcess> {

    public FunctionNodeProcessScreen(PluginMenuContext<FunctionNodeProcess> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
        collectImageAsset(EtherAdaptNodeAsset.PROGRESS_INDICATOR, 108, 96);
        collectImageAsset(EtherAdaptNodeAsset.ETHER_BAR_CTR, 26, 38);
    }

    private FunctionNodeProcess.ProgressMenuContext ctx() {
        return (FunctionNodeProcess.ProgressMenuContext) context;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        UIUtil.nineSliced(graphics, EtherAdaptNodeAsset.INFO_PANEL, lx(93), ly(15), 75, 48, 1);
        EtherAdaptNodeAsset.PROGRESS_INDICATOR_FILL.blit(graphics, lx(108), ly(96), 0, 0,
                plugin.progressing * EtherAdaptNodeAsset.PROGRESS_INDICATOR_FILL.w / Config.nodeProcessMaxProgress,
                EtherAdaptNodeAsset.PROGRESS_INDICATOR_FILL.h
        );
        if (screen.getMenu().entity != null)
            UIUtil.renderEtherBarProgress(
                    screen.getMenu().entity.getEther(),
                    screen.getMenu().entity.getMaxEther(),
                    lx(27), ly(39), EtherAdaptNodeAsset.ETHER_BAR_CTR.w - 2, 2, graphics
            );
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public void createWidget() {
        FilterGuiRegClient.widget(screen, plugin.inputItemFilter.whitelist, FunctionNodeProcess.FILTER_PREFIX);
        screen.addRenderableWidget(new IASwitchButton(
                lx(15), ly(96),
                EtherAdaptNodeAsset.BTN_BLANK,
                EtherAdaptNodeAsset.BTN_BLANK_HOVER,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN_HOVER,
                null,
                Component.translatable("ether_craft.gui.node.function_node_process.show_input_filter"),
                Component.translatable("ether_craft.gui.node.function_node_process.show_input_filter"),
                (a) -> {
                    ctx().filterSlots.forEach(t -> t.setActive(a));
                    ctx().inputSlots.forEach(t -> t.setActive(!a));
                    return true;
                }
        ));
        screen.addRenderableWidget(new AbstractWidget(lx(134), ly(94), 18, 18, Component.empty()) {
            @Override
            protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int x, int y, float v) {
                if (isMouseOver(x, y)) {
                    EtherAdaptNodeAsset.SLOT_RESULT_INDICATOR_HOVER.blit(guiGraphicsExtractor, getX(), getY());
                }
                EtherAdaptNodeAsset.SLOT_RESULT_INDICATOR.blit(guiGraphicsExtractor, getX(), getY());
                ItemStack item = plugin.targetItemFilter.getItem(0);
                if (!item.isEmpty()) {
                    guiGraphicsExtractor.item(item, getX(), getY());
                }
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            }

            @Override
            public void onClick(MouseButtonEvent event, boolean doubleClick) {
                ItemStack carried = screen.getMenu().getCarried();
                ClientPacketDistributor.sendToServer(new SetFilterSlotC2S(ctx().targetFilterIdx, 0, carried));
            }
        });
    }
}