package studio.fantasyit.ether_craft.node.tabs.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.ScreenMenuSyncer;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegClient;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.feature.DestructionUpgrade;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;
import studio.fantasyit.ether_craft.util.UIUtil;

public class DestructionTab extends BaseEtherNodeTabWidgetProvider<DestructionUpgrade> {
    private static final int INFO_PANEL_X = 93;
    private static final int INFO_PANEL_Y = 15;
    private static final int INFO_PANEL_W = 75;
    private static final int INFO_PANEL_H = 48;
    private static final int TEXT_LINE_1_Y = INFO_PANEL_Y + 4;
    private static final int TEXT_LINE_2_Y = INFO_PANEL_Y + 15;
    private static final int TEXT_LINE_3_Y = INFO_PANEL_Y + 26;

    private IASwitchButton modeButton;

    public DestructionTab(PluginMenuContext<DestructionUpgrade> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
    }

    @Override
    public void createWidget() {
        FilterGuiRegClient.widget(screen, () -> plugin.filter.whitelist, plugin.installedId);

        modeButton = new IASwitchButton(
                lx(15), ly(113),
                EtherAdaptNodeAsset.BTN_BLANK,
                EtherAdaptNodeAsset.BTN_BLANK_HOVER,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN,
                EtherAdaptNodeAsset.BTN_BLANK_DOWN_HOVER,
                null,
                Component.translatable("ether_craft.gui.node.destruction.mode_overflow"),
                Component.translatable("ether_craft.gui.node.destruction.mode_all"),
                t -> {
                    ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                            plugin.installedId,
                            DestructionUpgrade.SYNC_MODE,
                            0, (!t) ? 1 : 0));
                    updateModeIcon(!t);
                    return true;
                }
        );
        modeButton.setDown(plugin.destroyMode == DestructionUpgrade.DestroyMode.ALL);
        screen.addRenderableWidget(modeButton);
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(
                () -> plugin.destroyMode == DestructionUpgrade.DestroyMode.ALL,
                v -> {
                    modeButton.setDown(v);
                    updateModeIcon(v);
                }));
        updateModeIcon();
    }

    private void updateModeIcon() {
        updateModeIcon(plugin.destroyMode == DestructionUpgrade.DestroyMode.ALL);
    }

    private void updateModeIcon(boolean destroyAll) {
        if (modeButton == null) return;
        modeButton.setIcon(destroyAll
                ? EtherAdaptNodeAsset.BTN_ICON_DESTROY_ALL
                : EtherAdaptNodeAsset.BTN_ICON_OVERFLOW);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);

        UIUtil.nineSliced(graphics, EtherAdaptNodeAsset.INFO_PANEL,
                lx(INFO_PANEL_X), ly(INFO_PANEL_Y), INFO_PANEL_W, INFO_PANEL_H, 1);

        boolean destroyAll = plugin.destroyMode == DestructionUpgrade.DestroyMode.ALL;
        boolean whitelist = plugin.filter.whitelist;
        Font font = Minecraft.getInstance().font;
        int x = lx(INFO_PANEL_X) + 4;
        graphics.text(font, Component.translatable(destroyAll
                ? "ether_craft.gui.node.destruction.line_1_all"
                : "ether_craft.gui.node.destruction.line_1_overflow"), x, ly(TEXT_LINE_1_Y), 0xFFFFFFFF);
        graphics.text(font, Component.translatable(whitelist
                ? "ether_craft.gui.node.destruction.line_2_whitelist"
                : "ether_craft.gui.node.destruction.line_2_blacklist"), x, ly(TEXT_LINE_2_Y), 0xFFFFFFFF);
        graphics.text(font, Component.translatable(destroyAll
                ? "ether_craft.gui.node.destruction.line_3_all"
                : "ether_craft.gui.node.destruction.line_3_overflow"), x, ly(TEXT_LINE_3_Y), 0xFFFFFFFF);
    }
}
