package studio.fantasyit.ether_craft.node.plugins.feature;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.upgrade.EtherStreamSpeedDownUpgrade;
import studio.fantasyit.ether_craft.node.plugins.upgrade.EtherStreamSpeedUpUpgrade;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamStorageCapability;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStreamHolderManager;

import java.util.Optional;

public class FeatureEtherStreamEmitter extends AbstractDirectionalFilterFeature {
    public static class MenuContext extends PluginMenuContext<FeatureEtherStreamEmitter> {
        public MenuContext(EtherAdaptNodeContainerMenu menu, FeatureEtherStreamEmitter plugin) {
            super(menu, plugin);
            menu.addDataSlot(new BaseDataSlot(() -> scrollMin, t -> scrollMin = t));
            menu.addDataSlot(new BaseDataSlot(() -> scrollMax, t -> scrollMax = t));
            scrollMin = Config.nodeEmitterMinEtherMin;
            scrollMax = Math.toIntExact(Math.min(Config.nodeEmitterMinEtherMax, menu.entity.getMaxEther()));
        }

        public int scrollMin;
        public int scrollMax;
    }

    public static final Identifier ID = EtherCraft.id("ether_stream_emitter");
    public static final Identifier SYNC_MIN_ETHER = EtherCraft.id("emitter/min_ether");

    public int minEther = 1000;

    public FeatureEtherStreamEmitter(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    public void tickOutput() {
        queueWithCd(ID, 5, this::process);
    }

    private boolean process() {
        if (direction != null && nodeEntity.getEther() >= minEther) {
            long sendWith = Math.min(Integer.MAX_VALUE, nodeEntity.getEther());
            nodeEntity.extractEther(sendWith);
            Vec3 dir = direction.getUnitVec3().multiply(0.55f, 0.55f, 0.55f);
            PosDir posDir = new PosDir(nodeEntity.getBlockPos(), direction);
            if (!(nodeEntity.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return false;

            VirtualEtherStreamHolderManager veshm = VirtualEtherStreamHolderManager.get(serverLevel);
            if (!veshm.canCreateStream(posDir)) return false;
            Vec3 spd = direction.getUnitVec3().multiply(0.055f, 0.055f, 0.055f);
            for (int i = 0; i < nodeEntity.featureUpgradeStorage.getContainerSize(); i++) {
                @Nullable Identifier plugin = nodeEntity.featureUpgradeStorage.getPluginId(i);
                if (EtherStreamSpeedUpUpgrade.ID.equals(plugin)) {
                    spd = spd.multiply(2f, 2f, 2f);
                } else if (EtherStreamSpeedDownUpgrade.ID.equals(plugin)) {
                    spd = spd.multiply(0.5f, 0.5f, 0.5f);
                }
            }
            IEtherStreamLike stream = veshm.createStream(
                    serverLevel, posDir, (int) sendWith,
                    nodeEntity.getBlockPos().getCenter().add(dir),
                    spd
            );

            @Nullable AbstractNodePlugin mainPlugin = nodeEntity.functionStorage.getPlugin(0);
            if (mainPlugin instanceof IEtherStreamCapabilityProviderPlugin provider)
                provider.provideCapabilities(stream);
            for (int i = 0; i < nodeEntity.featureUpgradeStorage.getContainerSize(); i++) {
                AbstractNodePlugin plugin = nodeEntity.featureUpgradeStorage.getPlugin(i);
                if (plugin instanceof IEtherStreamCapabilityProviderPlugin provider) {
                    provider.provideCapabilities(stream);
                }
            }

            Optional<IStreamCapability> optCap = stream.getCapability(EtherStreamStorageCapability.ID);
            if (optCap.isPresent() && optCap.get() instanceof EtherStreamStorageCapability itemCapability) {
                try (Transaction transaction = Transaction.openRoot()) {
                    for (int i = 0; i < itemCapability.getContainerSize(); i++) {
                        ItemStack itemStack = nodeEntity.extractWithPredicate(filter::accepts, transaction, Integer.MAX_VALUE);
                        if (itemStack.isEmpty()) {
                            break;
                        }
                        itemCapability.setItem(i, itemStack);
                    }
                    transaction.commit();
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("minEther", Codec.INT, minEther);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        minEther = input.read("minEther", Codec.INT).orElse(1000);
        nodeEntity.setSyncedPluginData(installedId, SYNC_MIN_ETHER, minEther);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        super.syncScreenData(message);
        if (message.id().equals(SYNC_MIN_ETHER)) {
            minEther = Math.clamp(message.data(), Config.nodeEmitterMinEtherMin, Config.nodeEmitterMinEtherMax);
            nodeEntity.setSyncedPluginData(installedId, SYNC_MIN_ETHER, minEther);
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public PluginMenuContext<?> makeContext(EtherAdaptNodeContainerMenu etherAdaptNodeContainerMenu) {
        return new MenuContext(etherAdaptNodeContainerMenu, this);
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> minEther, t -> minEther = t));
    }
}
