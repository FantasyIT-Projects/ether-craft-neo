package studio.fantasyit.ether_craft.node.tabs.function;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.menu.base.widget.ScrollableWidget;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.ScreenMenuSyncer;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionCreativeEther;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;

import java.util.List;

public class CreativeEtherScreen extends BaseEtherNodeTabWidgetProvider<FunctionCreativeEther> {
    private static final int SLIDER_X = 20;
    private static final int SLIDER_Y = 15;

    private ScrollableWidget scroll;
    private int currentMaxEther;

    public CreativeEtherScreen(PluginMenuContext<FunctionCreativeEther> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
    }

    @Override
    public void createWidget() {
        int maxEther = screen.getMenu().entity.nodeProperty.maxEther;
        currentMaxEther = maxEther;
        scroll = new ScrollableWidget(
                lx(SLIDER_X), ly(SLIDER_Y),
                maxEther - 1,
                EtherAdaptNodeAsset.SCROLL_BACK,
                EtherAdaptNodeAsset.SCROLL_BLOCK,
                EtherAdaptNodeAsset.SCROLL_BLOCK_HOVER,
                EtherAdaptNodeAsset.SCROLL_BLOCK_HOVER,
                v -> {
                    int actual = v + 1;
                    ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                            plugin.installedId, FunctionCreativeEther.SYNC_VALUE, 0, actual));
                }
        );
        screen.addRenderableWidget(scroll);
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(
                () -> plugin.fillAmount - 1,
                scroll::setValue
        ));
        collectTooltipArea(
                new Rect2i(lx(SLIDER_X), ly(SLIDER_Y),
                        EtherAdaptNodeAsset.SCROLL_BLOCK.w, EtherAdaptNodeAsset.SCROLL_BACK.h),
                () -> List.of(Component.translatable("menu.ether_craft.scroll_tooltip"))
        );
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(screen.getMinecraft().font,
                Component.translatable("menu.ether_craft.node.creative_ether.label"),
                lx(SLIDER_X) + 5, ly(SLIDER_Y) - 10, 0xFFFFFFFF);
        graphics.centeredText(screen.getMinecraft().font,
                Component.translatable("menu.ether_craft.node.creative_ether.value", scroll.getValue() + 1),
                lx(SLIDER_X) + 5, ly(SLIDER_Y) + EtherAdaptNodeAsset.SCROLL_BACK.h + 2, 0xFFFFFFFF);
    }

    @Override
    public void tick() {
        int maxEther = screen.getMenu().entity.nodeProperty.maxEther;
        if (currentMaxEther != maxEther) {
            currentMaxEther = maxEther;
            scroll.setMaxValue(maxEther - 1);
        }
    }
}
