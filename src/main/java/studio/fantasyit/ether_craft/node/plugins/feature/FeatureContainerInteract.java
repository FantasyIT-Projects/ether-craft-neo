package studio.fantasyit.ether_craft.node.plugins.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import javax.annotation.Nullable;

public class FeatureContainerInteract extends AbstractDirectionalFilterFeature {
    public static final Identifier ID = EtherCraft.id("container_interact");
    public static final Identifier SYNC_EXTRACT_MODE = EtherCraft.id("container_interact/extract_mode");
    public static final Identifier WORKING_MODE = EtherCraft.id("container_interact/working_mode");

    public boolean extractMode = true;

    public FeatureContainerInteract(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    public void tickInput() {
        if (direction != null)
            if (extractMode)
                queueWithCd(ID, 1, this::process);
    }

    @Override
    public void tickOutput() {
        if (direction != null)
            if (!extractMode)
                queueWithCd(ID, 1, this::process);
    }

    private boolean process() {
        if (nodeEntity.getEther() < Config.nodeContainerInteractEtherPerItem)
            return false;
        if (direction == null) {
            return true;
        }
        Level level = nodeEntity.getLevel();
        if (level == null || level.isClientSide()) {
            return true;
        }
        BlockPos targetPos = nodeEntity.getBlockPos().relative(direction);
        ResourceHandler<ItemResource> adjacentHandler = level.getCapability(
                Capabilities.Item.BLOCK,
                targetPos,
                direction.getOpposite()
        );
        if (adjacentHandler == null) {
            return true;
        }

        if (extractMode) {
            tryTransfer(adjacentHandler, nodeEntity, nodeEntity);
        } else {
            tryTransfer(nodeEntity, adjacentHandler, nodeEntity);
        }
        return true;
    }

    private void tryTransfer(ResourceHandler<ItemResource> fromHandler, ResourceHandler<ItemResource> targetHandler, EtherContainer etherSource) {
        long costPerItem = Config.nodeContainerInteractEtherPerItem;
        if (etherSource.getEther() < costPerItem)
            return;
        try (Transaction transaction = Transaction.openRoot()) {
            for (int i = 0; i < fromHandler.size(); i++) {
                ItemResource resource = fromHandler.getResource(i);
                if (resource.isEmpty()) {
                    continue;
                }
                if (!filter.accepts(resource)) {
                    continue;
                }
                if (fromHandler instanceof EtherAdaptNodeEntity ean && !ean.allowInteract(resource))
                    continue;
                int maxToExtract = maxToTransfer(resource, i, fromHandler, targetHandler, etherSource, transaction);
                if (maxToExtract <= 0) {
                    continue;
                }
                int extracted = fromHandler.extract(i, resource, maxToExtract, transaction);
                if (extracted <= 0) {
                    continue;
                }
                long totalCost = (long) extracted * costPerItem;
                if (etherSource.getEther() < totalCost) {
                    return;
                }
                if (targetHandler.insert(resource, extracted, transaction) < extracted) {
                    continue;
                }
                transaction.commit();
                etherSource.extractEther(totalCost);
                return;
            }
        }
    }

    private int maxToTransfer(ItemResource itemResource, int fromIdx, ResourceHandler<ItemResource> from, ResourceHandler<ItemResource> to, EtherContainer etherSource, @Nullable TransactionContext parent) {
        if (itemResource.is(ItemRegistry.ETHER)) {
            return (int) Math.floor((double) etherSource.getEther() / (Config.nodeContainerInteractEtherPerItem + Config.etherConvert));
        }
        int maxToExtract = Math.toIntExact(etherSource.getEther() / Config.nodeContainerInteractEtherPerItem);
        try (Transaction t1 = Transaction.open(parent)) {
            int t = from.extract(fromIdx, itemResource, maxToExtract, t1);
            if (t < maxToExtract)
                maxToExtract = t;
            if (maxToExtract > 0) {
                t = to.insert(itemResource, maxToExtract, t1);
                if (t < maxToExtract)
                    maxToExtract = t;
            }
        }
        return maxToExtract;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("extractMode", Codec.INT, extractMode ? 1 : 0);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        extractMode = input.read("extractMode", Codec.INT).orElse(1) == 1;
        nodeEntity.setSyncedPluginData(installedId, WORKING_MODE, extractMode ? 1 : 0);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        super.syncScreenData(message);
        if (message.id().equals(SYNC_EXTRACT_MODE)) {
            extractMode = message.data() == 1;
            nodeEntity.setSyncedPluginData(installedId, WORKING_MODE, extractMode ? 1 : 0);
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> extractMode ? 1 : 0, t -> extractMode = t == 1));
    }
}
