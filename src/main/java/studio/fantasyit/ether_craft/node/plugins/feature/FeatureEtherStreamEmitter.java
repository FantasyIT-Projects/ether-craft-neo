package studio.fantasyit.ether_craft.node.plugins.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.*;
import studio.fantasyit.ether_craft.node.plugins.upgrade.EtherStreamSpeedDownUpgrade;
import studio.fantasyit.ether_craft.node.plugins.upgrade.EtherStreamSpeedUpUpgrade;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.PosDir;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamStorageCapability;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStreamHolderManager;

import java.util.Optional;

public class FeatureEtherStreamEmitter extends AbstractDirectionalFilterFeature implements ITickOutputPlugin, IOnBlockUpdatePlugin {
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

    private @Nullable EtherContainer targetContainer = null;
    private boolean isTargetFullBlock = false;
    private boolean hasCapProvider = false;

    public FeatureEtherStreamEmitter(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    public void tickOutput() {
        if (direction != null)
            queueWithCd(ID, 5, this::process);
    }

    private boolean process() {
        if (direction != null && nodeEntity.getEther() >= minEther) {
            if (!(nodeEntity.getLevel() instanceof ServerLevel serverLevel)) return false;

            // 快速路径：发射面为完整方块且无任何插件为以太流提供能力
            if (isTargetFullBlock && !hasCapProvider) {
                return fastProcess();
            }

            long sendWith = Math.min(Integer.MAX_VALUE, nodeEntity.getEther());
            nodeEntity.extractEther(sendWith);
            PosDir posDir = new PosDir(nodeEntity.getBlockPos(), direction);
            VirtualEtherStreamHolderManager veshm = VirtualEtherStreamHolderManager.get(serverLevel);
            if (!veshm.canCreateStream(posDir)) return false;
            float spd = 0.055f;
            for (int i = 0; i < nodeEntity.featureUpgradeStorage.getContainerSize(); i++) {
                @Nullable Identifier plugin = nodeEntity.featureUpgradeStorage.getPluginId(i);
                if (EtherStreamSpeedUpUpgrade.ID.equals(plugin)) {
                    spd *= 2f;
                } else if (EtherStreamSpeedDownUpgrade.ID.equals(plugin)) {
                    spd *= 0.5f;
                }
            }
            IEtherStreamLike stream = veshm.createStream(
                    serverLevel, posDir, (int) sendWith,
                    0.55f,
                    spd
            );

            @Nullable AbstractNodePlugin mainPlugin = nodeEntity.functionStorage.getPlugin(0);
            if (mainPlugin instanceof IEtherStreamCapabilityProviderPlugin provider)
                provider.provideCapabilities(stream, stream.getExtraProperty());
            for (int i = 0; i < nodeEntity.featureUpgradeStorage.getContainerSize(); i++) {
                AbstractNodePlugin plugin = nodeEntity.featureUpgradeStorage.getPlugin(i);
                if (plugin instanceof IEtherStreamCapabilityProviderPlugin provider) {
                    provider.provideCapabilities(stream, stream.getExtraProperty());
                }
            }

            Optional<IStreamCapability> optCap = stream.getCapability(EtherStreamStorageCapability.ID);
            if (optCap.isPresent() && optCap.get() instanceof EtherStreamStorageCapability itemCapability) {
                for (int i = 0; i < itemCapability.getContainerSize(); i++) {
                    ItemStack itemStack = nodeEntity.extractWithPredicate(filter::accepts, Integer.MAX_VALUE);
                    if (itemStack.isEmpty()) {
                        break;
                    }
                    itemCapability.setItem(i, itemStack);
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
            nodeEntity.markChanged();
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

    @Override
    public void onBlockUpdate() {
        refreshTargetCache();
    }

    @Override
    public void update() {
        refreshTargetCache();
    }

    private void refreshTargetCache() {
        if (direction == null) {
            isTargetFullBlock = false;
            targetContainer = null;
            return;
        }
        BlockPos targetPos = nodeEntity.getBlockPos().relative(direction);
        Level level = nodeEntity.getLevel();
        if (level == null || level.isClientSide()) {
            isTargetFullBlock = false;
            targetContainer = null;
            return;
        }
        BlockState state = level.getBlockState(targetPos);
        boolean full = !state.isAir()
                && !state.is(Tags.ETHER_STREAM_PASS_THROUGH)
                && Block.isShapeFullBlock(state.getCollisionShape(level, targetPos));
        isTargetFullBlock = full;
        targetContainer = full ? level.getCapability(EtherContainer.ETHER_CONTAINER, targetPos) : null;
        hasCapProvider = computeHasCapabilityProvider();
    }

    private boolean fastProcess() {
        long sendWith = nodeEntity.getEther();
        nodeEntity.extractEther(sendWith);
        if (targetContainer != null) {
            targetContainer.receiveEther(sendWith);
        }
        return true;
    }

    private boolean computeHasCapabilityProvider() {
        if (nodeEntity.functionStorage.getPlugin(0) instanceof IEtherStreamCapabilityProviderPlugin) return true;
        for (int i = 0; i < nodeEntity.featureUpgradeStorage.getContainerSize(); i++)
            if (nodeEntity.featureUpgradeStorage.getPlugin(i) instanceof IEtherStreamCapabilityProviderPlugin)
                return true;
        return false;
    }
}
