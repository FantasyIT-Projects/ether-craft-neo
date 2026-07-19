package studio.fantasyit.ether_craft.node.tabs.function;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.menu.base.widget.ScrollableWidget;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.ScreenMenuSyncer;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionMute;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;

import java.util.List;
import java.util.function.Supplier;

public class MuteScreen extends BaseEtherNodeTabWidgetProvider<FunctionMute> {
    private static final int[][] SCROLL_POS = {
            {20, 5}, {40, 5}, {60, 5},
    };
    private static final Identifier[] SYNC_IDS = {
            FunctionMute.SYNC_RX, FunctionMute.SYNC_RY, FunctionMute.SYNC_RZ,
    };

    private final ScrollableWidget[] scrolls = new ScrollableWidget[3];

    public MuteScreen(PluginMenuContext<FunctionMute> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
    }

    @Override
    public void createWidget() {
        Supplier<Integer>[] startValues = new Supplier[]{
                () -> plugin.rx,
                () -> plugin.ry,
                () -> plugin.rz,
        };

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            scrolls[i] = new ScrollableWidget(
                    lx(SCROLL_POS[i][0]), ly(SCROLL_POS[i][1]),
                    Config.nodeMuteMaxRange,
                    EtherAdaptNodeAsset.SCROLL_BACK,
                    EtherAdaptNodeAsset.SCROLL_BLOCK,
                    EtherAdaptNodeAsset.SCROLL_BLOCK_HOVER,
                    EtherAdaptNodeAsset.SCROLL_BLOCK_HOVER,
                    v -> ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                            plugin.installedId, SYNC_IDS[idx], 0, v))
            );
            screen.addRenderableWidget(scrolls[i]);
            screen.registerMenuSyncer(new ScreenMenuSyncer<>(startValues[i], scrolls[i]::setValue));
            collectTooltipArea(
                    new Rect2i(lx(SCROLL_POS[i][0]), ly(SCROLL_POS[i][1]),
                            EtherAdaptNodeAsset.SCROLL_BLOCK.w, EtherAdaptNodeAsset.SCROLL_BACK.h),
                    () -> List.of(Component.translatable("menu.ether_craft.scroll_tooltip"))
            );
        }
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        String[] labels = {"rx", "ry", "rz"};
        for (int i = 0; i < 3; i++) {
            graphics.centeredText(screen.getMinecraft().font,
                    Component.translatable("menu.ether_craft.node.mute." + labels[i]),
                    lx(SCROLL_POS[i][0]) + 5, ly(SCROLL_POS[i][1]) + EtherAdaptNodeAsset.SCROLL_BACK.h + 2, 0xFFFFFFFF);
            graphics.centeredText(screen.getMinecraft().font,
                    Component.translatable("menu.ether_craft.node.mute.value", scrolls[i].getValue()),
                    lx(SCROLL_POS[i][0]) + 5, ly(SCROLL_POS[i][1]) + EtherAdaptNodeAsset.SCROLL_BACK.h + 10, 0xFFFFFFFF);
        }
    }
}