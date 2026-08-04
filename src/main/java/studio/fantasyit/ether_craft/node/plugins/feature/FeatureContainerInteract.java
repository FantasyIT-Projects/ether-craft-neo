package studio.fantasyit.ether_craft.node.plugins.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

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
                queueWithCd(ID, Config.nodeContainerInteractCd, this::process);
    }

    @Override
    public void tickOutput() {
        if (direction != null)
            if (!extractMode)
                queueWithCd(ID, Config.nodeContainerInteractCd, this::process);
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
        BlockEntity be = level.getBlockEntity(targetPos);
        if (be instanceof EtherAdaptNodeEntity targetNode) {
            if (extractMode)
                fastTransfer(targetNode, nodeEntity);
            else
                fastTransfer(nodeEntity, targetNode);
            return true;
        }
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

    private void fastTransfer(EtherAdaptNodeEntity fromNode, EtherAdaptNodeEntity toNode) {
        long costPerItem = Config.nodeContainerInteractEtherPerItem;
        int maxTransfer = (int) (nodeEntity.getEther() / costPerItem);

        if (fromNode.nodeProperty.itemifyEther) {
            int etherMaxTransfer = (int) (nodeEntity.getEther() / (costPerItem + Config.etherConvert));
            if (etherMaxTransfer > 0) {
                ItemStack etherStack = fromNode.etherStorage.removeItem(0, etherMaxTransfer);
                if (!etherStack.isEmpty()) {
                    ItemStack remaining = toNode.etherStorage.insertItemStack(etherStack);
                    int transferred = etherStack.getCount() - remaining.getCount();
                    if (!remaining.isEmpty())
                        fromNode.etherStorage.setItem(0, remaining);
                    if (transferred > 0) {
                        nodeEntity.extractEther((long) transferred * costPerItem);
                        fromNode.setChanged();
                        toNode.setChanged();
                        return;
                    }
                }
            }
        }

        Set<ItemResource> tried = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int slot = 0; slot < fromNode.normalStorage.getContainerSize(); slot++) {
            ItemStack stack = fromNode.normalStorage.getItem(slot);
            if (stack.isEmpty())
                continue;
            ItemResource resource = ItemResource.of(stack);
            if (!tried.add(resource))
                continue;
            if (!filter.accepts(stack))
                continue;
            if (!fromNode.allowInteract(stack))
                continue;

            int remaining = Math.min(stack.getCount(), maxTransfer);
            int totalConsumed = toNode.insertStack(stack.copyWithCount(remaining), remaining);
            int leftInSource = stack.getCount() - totalConsumed;
            fromNode.normalStorage.setItem(slot, stack.copyWithCount(leftInSource));

            if (totalConsumed > 0) {
                nodeEntity.extractEther((long) totalConsumed * costPerItem);
                fromNode.setChanged();
                toNode.setChanged();
                return;
            }
        }
    }

    private void tryTransfer(ResourceHandler<ItemResource> fromHandler, ResourceHandler<ItemResource> targetHandler, EtherContainer etherSource) {
        long costPerItem = Config.nodeContainerInteractEtherPerItem;
        if (etherSource.getEther() < costPerItem)
            return;
        Set<ItemResource> tried = Collections.newSetFromMap(new IdentityHashMap<>());
        try (Transaction transaction = Transaction.openRoot()) {
            for (int i = 0; i < fromHandler.size(); i++) {
                ItemResource resource = fromHandler.getResource(i);
                if (resource.isEmpty()) {
                    continue;
                }
                if (!tried.add(resource)) {
                    continue;
                }
                if (!filter.accepts(resource)) {
                    continue;
                }
                if (fromHandler instanceof EtherAdaptNodeEntity ean && !ean.allowInteract(resource))
                    continue;
                int maxAffordable = Math.toIntExact(etherSource.getEther() / costPerItem);
                if (etherSource == fromHandler && resource.is(ItemRegistry.ETHER)) {
                    maxAffordable = (int) Math.floor((double) etherSource.getEther() / (costPerItem + Config.etherConvert));
                }
                if (maxAffordable <= 0) {
                    continue;
                }
                int available;
                try (Transaction t1 = Transaction.open(transaction)) {
                    available = fromHandler.extract(i, resource, maxAffordable, t1);
                }
                if (available <= 0) {
                    continue;
                }
                int inserted = targetHandler.insert(resource, available, transaction);
                if (inserted <= 0) {
                    continue;
                }
                int extracted = fromHandler.extract(i, resource, inserted, transaction);
                if (extracted < inserted) {
                    continue;
                }
                transaction.commit();
                etherSource.extractEther((long) inserted * costPerItem);
                return;
            }
        }
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
            nodeEntity.setChanged();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> extractMode ? 1 : 0, t -> extractMode = t == 1));
    }
}
