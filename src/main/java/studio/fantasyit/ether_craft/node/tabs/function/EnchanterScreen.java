package studio.fantasyit.ether_craft.node.tabs.function;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.ScreenMenuSyncer;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionEnchanter;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;
import studio.fantasyit.ether_craft.util.UIUtil;

public class EnchanterScreen extends BaseEtherNodeTabWidgetProvider<FunctionEnchanter> {

    private static final ImageAsset[] LEVEL_ICONS = {
            EtherAdaptNodeAsset.ENCHANT_ICON_LV1,
            EtherAdaptNodeAsset.ENCHANT_ICON_LV2,
            EtherAdaptNodeAsset.ENCHANT_ICON_LV3
    };
    private static final ImageAsset[] LEVEL_ICONS_DISABLED = {
            EtherAdaptNodeAsset.ENCHANT_ICON_LV1_DISABLED,
            EtherAdaptNodeAsset.ENCHANT_ICON_LV2_DISABLED,
            EtherAdaptNodeAsset.ENCHANT_ICON_LV3_DISABLED
    };
    private static final int[] BUTTON_Y = {15, 31, 47};
    private final IASwitchButton[] levelButtons = new IASwitchButton[3];
    private final boolean[] levelButtonsEnabled = new boolean[3];

    public EnchanterScreen(PluginMenuContext<FunctionEnchanter> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
        collectImageAsset(EtherAdaptNodeAsset.ETHER_BAR_CTR, 26, 38);
        collectImageAsset(EtherAdaptNodeAsset.PROGRESS_INDICATOR, 50, 46);
    }

    @Override
    public void createWidget() {
        for (int i = 0; i < 3; i++) {
            final int level = i;
            levelButtons[i] = new IASwitchButton(
                    lx(74), ly(BUTTON_Y[i]),
                    EtherAdaptNodeAsset.ENCHANT_BTN_NORMAL,
                    EtherAdaptNodeAsset.ENCHANT_BTN_HOVER,
                    EtherAdaptNodeAsset.ENCHANT_BTN_DOWN,
                    EtherAdaptNodeAsset.ENCHANT_BTN_DOWN,
                    null,
                    Component.translatable("ether_craft.gui.node.enchanter.level_cost", level, Config.nodeEnchanterEtherCosts.get(i)),
                    Component.translatable("ether_craft.gui.node.enchanter.level_cost", level, Config.nodeEnchanterEtherCosts.get(i)),
                    down -> {
                        if (!levelButtonsEnabled[level])
                            return false;
                        ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                                plugin.installedId, FunctionEnchanter.SYNC_LEVEL, 0, down ? -1 : level));
                        return true;
                    }
            );
            screen.addRenderableWidget(levelButtons[i]);
        }
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> plugin.selectedLevel, _ -> updateButtonStates()));
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (screen.getMenu().entity == null) return;
        long maxEther = screen.getMenu().entity.getMaxEther();
        for (int i = 0; i < 3; i++) {
            levelButtons[i].setDown(plugin.selectedLevel == i);
            if (Config.nodeEnchanterEtherCosts.size() > i && (maxEther == 0 || maxEther >= Config.nodeEnchanterEtherCosts.get(i))) {
                levelButtons[i].setIcon(LEVEL_ICONS[i]);
                levelButtonsEnabled[i] = true;
            } else {
                levelButtons[i].setIcon(LEVEL_ICONS_DISABLED[i]);
                levelButtonsEnabled[i] = false;
            }
        }
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
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        int level = plugin.selectedLevel;
        int progress = plugin.progress;
        if (level > 0 && progress > 0 && progress < Config.nodeEnchanterMaxProgress) {
            EtherAdaptNodeAsset.PROGRESS_INDICATOR_FILL.blit(
                    graphics, lx(50), ly(46), 0, 0,
                    progress * EtherAdaptNodeAsset.PROGRESS_INDICATOR_FILL.w / Config.nodeEnchanterMaxProgress,
                    EtherAdaptNodeAsset.PROGRESS_INDICATOR_FILL.h
            );
        }
    }
}
