package studio.fantasyit.ether_craft.node.plugins.feature;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
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
    public boolean enabled = true;

    public FeatureRedstoneSignal(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    public int getSignal() {
        if (!enabled) return 0;
        if (mode == SignalMode.ETHER) {
            long ether = nodeEntity.getEther();
            long maxEther = nodeEntity.getMaxEther();
            if (maxEther <= 0) return 0;
            return (int) (ether * 15 / maxEther);
        } else {
            int unlocked = nodeEntity.nodeProperty.slotUnlock;
            if (unlocked <= 0) return 0;
            int filled = 0;
            for (int i = 0; i < unlocked; i++) {
                if (!nodeEntity.normalStorage.getItem(i).isEmpty())
                    filled++;
            }
            return filled * 15 / unlocked;
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("rssMode", SignalMode.CODEC, mode);
        output.putBoolean("rssEnabled", enabled);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mode = input.read("rssMode", SignalMode.CODEC).orElse(SignalMode.ETHER);
        enabled = input.getBooleanOr("rssEnabled", true);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        super.syncScreenData(message);
        if (message.id().equals(SYNC_MODE)) {
            mode = message.data() == 1 ? SignalMode.INVENTORY : SignalMode.ETHER;
            nodeEntity.pluginUpdate();
        }
        if (message.id().equals(SYNC_ENABLED)) {
            enabled = message.data() == 1;
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> mode == SignalMode.INVENTORY ? 1 : 0, t -> mode = (t == 1 ? SignalMode.INVENTORY : SignalMode.ETHER)));
        menu.addDataSlot(new BaseDataSlot(() -> enabled ? 1 : 0, t -> enabled = t == 1));
    }
}
