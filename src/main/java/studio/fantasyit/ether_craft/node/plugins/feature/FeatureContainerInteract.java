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
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public class FeatureContainerInteract extends AbstractDirectionalFilterFeature {
    public static final Identifier ID = EtherCraft.id("container_interact");
    public static final Identifier SYNC_EXTRACT_MODE = EtherCraft.id("container_interact/extract_mode");
    public static final Identifier WORKING_MODE = EtherCraft.id("container_interact/working_mode");

    public boolean extractMode = true;

    public FeatureContainerInteract(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    public void tick() {
        queueWithCd(ID, 1, this::process);
    }

    private boolean process() {
        if (direction == null) {
            return false;
        }
        Level level = nodeEntity.getLevel();
        if (level == null || level.isClientSide()) {
            return false;
        }
        BlockPos targetPos = nodeEntity.getBlockPos().relative(direction);
        ResourceHandler<ItemResource> adjacentHandler = level.getCapability(
                Capabilities.Item.BLOCK,
                targetPos,
                direction.getOpposite()
        );
        if (adjacentHandler == null) {
            return false;
        }

        if (extractMode) {
            return pullFromAdjacent(adjacentHandler);
        } else {
            return pushToAdjacent(adjacentHandler);
        }
    }

    private boolean pullFromAdjacent(ResourceHandler<ItemResource> adjacentHandler) {
        long costPerItem = Config.containerInteractEtherPreItem;
        try (Transaction transaction = Transaction.openRoot()) {
            for (int i = 0; i < adjacentHandler.size(); i++) {
                ItemResource resource = adjacentHandler.getResource(i);
                if (resource.isEmpty()) {
                    continue;
                }
                if (!filter.accepts(resource)) {
                    continue;
                }
                int extracted = adjacentHandler.extract(i, resource, resource.getMaxStackSize(), transaction);
                if (extracted <= 0) {
                    continue;
                }
                long totalCost = (long) extracted * costPerItem;
                if (nodeEntity.getEther() < totalCost) {
                    return false;
                }
                int remaining = extracted;
                for (int j = 0; j < nodeEntity.size(); j++) {
                    if (nodeEntity.isValid(j, resource)) {
                        int inserted = nodeEntity.insert(j, resource, remaining, transaction);
                        remaining -= inserted;
                        if (remaining <= 0) {
                            break;
                        }
                    }
                }
                if (remaining > 0) {
                    return false;
                }
                transaction.commit();
                nodeEntity.extractEther(totalCost);
                return true;
            }
        }
        return false;
    }

    private boolean pushToAdjacent(ResourceHandler<ItemResource> adjacentHandler) {
        long costPerItem = Config.containerInteractEtherPreItem;
        try (Transaction transaction = Transaction.openRoot()) {
            for (int i = 0; i < nodeEntity.size(); i++) {
                ItemResource resource = nodeEntity.getResource(i);
                if (resource.isEmpty()) {
                    continue;
                }
                if (!filter.accepts(resource)) {
                    continue;
                }
                int extracted = nodeEntity.extract(i, resource, resource.getMaxStackSize(), transaction);
                if (extracted <= 0) {
                    continue;
                }
                long totalCost = (long) extracted * costPerItem;
                if (nodeEntity.getEther() < totalCost) {
                    return false;
                }
                int remaining = extracted;
                for (int j = 0; j < adjacentHandler.size(); j++) {
                    if (adjacentHandler.isValid(j, resource)) {
                        int inserted = adjacentHandler.insert(j, resource, remaining, transaction);
                        remaining -= inserted;
                        if (remaining <= 0) {
                            break;
                        }
                    }
                }
                if (remaining > 0) {
                    return false;
                }
                transaction.commit();
                nodeEntity.extractEther(totalCost);
                return true;
            }
        }
        return false;
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
        if (message.id().equals(SYNC_EXTRACT_MODE) && message.index() == installedId.id()) {
            extractMode = message.data() == 1;
            nodeEntity.setSyncedPluginData(installedId, WORKING_MODE, extractMode ? 1 : 0);
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> extractMode ? 1 : 0 , t -> extractMode = t == 1));
    }
}
