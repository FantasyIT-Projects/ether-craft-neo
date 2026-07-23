package studio.fantasyit.ether_craft.node.plugins.function;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.menu.node.slot.OversizedEtherSlot;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegCommon;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.upgrade.IGeneratorAdjuster;
import studio.fantasyit.ether_craft.util.ContainerOps;

public abstract class AbstractItemConsumeFunction extends AbstractNodePlugin {
    public static final Identifier WORKING_MATERIAL = EtherCraft.id("generator/material");
    public ItemFilter filter = new ItemFilter(21, nodeEntity::setChanged);
    public SimpleContainer container = new SimpleContainer(1);
    public int remainBurnTicks = 0;
    public int generatePreTick = 0;

    public enum WorkingMaterial {
        IDLE,
        ANY,
        STONE,
        WOOD,
        DEEPSLATE,
        COAL,
        LAVA
    }

    public AbstractItemConsumeFunction(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }


    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {
        if (nodeProperty.slotUnlock == 0)
            nodeProperty.slotUnlock = 1;
        nodeProperty.specialRenderer = true;
    }

    abstract boolean accepts(ItemResource stack);

    abstract IGeneratorAdjuster.AdjustedParameters onConsumeItem(ItemStack itemStack);

    @Override
    public void tickWork() {
        ItemStack oItemStack = container.getItem(0);
        int remain = oItemStack.isEmpty() ? 64 : oItemStack.getMaxStackSize() - oItemStack.getCount();
        try (var transaction = Transaction.openRoot()) {
            ItemStack toPlace = nodeEntity.extractWithPredicate(stack ->
                            accepts(stack) && container.canPlaceItem(0, stack.toStack()) && filter.accepts(stack),
                    transaction, remain
            );
            if (!toPlace.isEmpty() && (oItemStack.isEmpty() || ItemStack.isSameItemSameComponents(oItemStack, toPlace)) && oItemStack.getCount() + toPlace.getCount() <= toPlace.getMaxStackSize()) {
                ItemStack newStack = oItemStack.isEmpty() ? toPlace.copy() : oItemStack.copyWithCount(oItemStack.getCount() + toPlace.getCount());
                container.setItem(0, newStack);
                transaction.commit();
            }
        }

        if (remainBurnTicks <= 0 && !container.getItem(0).isEmpty()) {
            ItemStack toConsumeItemStack = container.getItem(0).copy();
            if (accepts(ItemResource.of(toConsumeItemStack))) {
                IGeneratorAdjuster.AdjustedParameters parameter = onConsumeItem(toConsumeItemStack);
                container.setItem(0, toConsumeItemStack);
                for (int i = 0; i < nodeEntity.featureUpgradeStorage.getContainerSize(); i++) {
                    if (nodeEntity.featureUpgradeStorage.hasPlugin(i) && nodeEntity.featureUpgradeStorage.getPlugin(i) instanceof IGeneratorAdjuster iga) {
                        parameter = iga.adjust(parameter);
                    }
                }
                generatePreTick = parameter.preTick();
                remainBurnTicks = parameter.burnTicks();
                nodeEntity.receiveEther(generatePreTick);
            }
        } else {
            if (remainBurnTicks > 0) {
                nodeEntity.receiveEther(generatePreTick);
                remainBurnTicks--;
            }
        }

    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        super.syncScreenData(message);
        FilterGuiRegCommon.sync(message, filter);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.store("container", ItemStack.OPTIONAL_CODEC.listOf(), ContainerOps.containerToItemList(container));
        output.store("remainBurnTicks", Codec.INT, remainBurnTicks);
        output.store("generatePreTick", Codec.INT, generatePreTick);
        filter.serialize(output.child("filter"));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        input.read("container", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> ContainerOps.fillContainerByItemList(container, l));
        input.read("remainBurnTicks", Codec.INT).ifPresent(i -> remainBurnTicks = i);
        input.read("preTick", Codec.INT).ifPresent(i -> generatePreTick = i);
        filter.deserialize(input.childOrEmpty("filter"));
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addSlotDraw(new OversizedEtherSlot(nodeEntity.etherStorage, 0, 28, 20));
        menu.addSlotDraw(new Slot(container, 0, 28, 44));
        menu.addDataSlot(new BaseDataSlot(() -> remainBurnTicks, (a) -> remainBurnTicks = a));
        FilterGuiRegCommon.slots(menu, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!container.isEmpty()) {
            ContainerOps.tryPlaceToItemHandler(container, nodeEntity);
            if (nodeEntity.getLevel() != null) {
                Containers.dropContents(nodeEntity.getLevel(), nodeEntity.getBlockPos(), container);
            }
        }
    }
}
