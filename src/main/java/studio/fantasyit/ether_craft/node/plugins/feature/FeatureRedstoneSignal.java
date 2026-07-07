package studio.fantasyit.ether_craft.node.plugins.feature;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public class FeatureRedstoneSignal extends AbstractDirectionalFeature {
    public static final Identifier ID = EtherCraft.id("redstone_signal");
    public static final Identifier SYNC_MODE = EtherCraft.id("redstone_signal/mode");
    public static final Identifier SYNC_ENABLED = EtherCraft.id("redstone_signal/enabled");

    public enum SignalMode {
        ETHER, INVENTORY;
        public static final Codec<SignalMode> CODEC = Codec.STRING.xmap(
                s -> s.equals("INVENTORY") ? INVENTORY : ETHER,
                m -> m == INVENTORY ? "INVENTORY" : "ETHER"
        );
    }

    public SignalMode mode = SignalMode.ETHER;
    public boolean revert = true;

    public FeatureRedstoneSignal(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    public int getSignal() {
        if (direction == null) return 0;
        if (mode == SignalMode.ETHER) {
            long ether = nodeEntity.getEther();
            long maxEther = nodeEntity.getMaxEther();
            if (maxEther <= 0) return revert ? 15 : 0;
            if (revert)
                return 15 - (int) (ether * 15 / maxEther);
            else
                return (int) (ether * 15 / maxEther);
        } else {
            int unlocked = nodeEntity.nodeProperty.slotUnlock;
            if (unlocked <= 0) return revert ? 15 : 0;
            int filled = 0;
            int total = 0;
            for (int i = 0; i < unlocked; i++) {
                ItemStack it = nodeEntity.normalStorage.getItem(i);
                if (!it.isEmpty()) {
                    filled += it.count();
                    total += it.getMaxStackSize();
                } else {
                    total += 64;
                }
            }
            if (revert)
                return 15 - filled * 15 / total;
            else
                return (int) Math.ceil(filled * 15.0 / total);
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("rssMode", SignalMode.CODEC, mode);
        output.putBoolean("rssEnabled", revert);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mode = input.read("rssMode", SignalMode.CODEC).orElse(SignalMode.ETHER);
        revert = input.getBooleanOr("rssEnabled", true);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        super.syncScreenData(message);
        if (message.id().equals(SYNC_MODE)) {
            mode = message.data() == 1 ? SignalMode.INVENTORY : SignalMode.ETHER;
            nodeEntity.pluginUpdate();
        }
        if (message.id().equals(SYNC_ENABLED)) {
            revert = message.data() == 1;
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> mode == SignalMode.INVENTORY ? 1 : 0, t -> mode = (t == 1 ? SignalMode.INVENTORY : SignalMode.ETHER)));
        menu.addDataSlot(new BaseDataSlot(() -> revert ? 1 : 0, t -> revert = t == 1));
    }
}
