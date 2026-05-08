package studio.fantasyit.ether_craft.block.node;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import oshi.util.tuples.Pair;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.block.base.ITickable;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.util.ContainerOps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static studio.fantasyit.ether_craft.register.BlockEntityRegistry.ETHER_NODE_ENTITY;

public class EtherAdaptNodeEntity extends BlockEntity implements ResourceHandler<@NotNull ItemResource>, EtherContainer, ITickable {
    public static final int UPGRADE_SIZE = 4;
    private final ResourceHandler<ItemResource> normalHandler;
    private boolean markUpdate = false;
    public final NodeProperty nodeProperty;
    public final EtherSlotSyncContainer etherStorage;
    public final EtherPluginUpgradeContainer functionStorage;
    public final EtherPluginUpgradeContainer featureUpgradeStorage;
    public final SimpleContainer normalStorage;


    public EtherAdaptNodeEntity(BlockPos worldPosition, BlockState blockState) {
        super(ETHER_NODE_ENTITY.get(), worldPosition, blockState);
        nodeProperty = new NodeProperty();
        etherStorage = new EtherSlotSyncContainer(this);
        normalStorage = new SimpleContainer(27);
        normalHandler = VanillaContainerWrapper.of(normalStorage);
        functionStorage = new EtherPluginUpgradeContainer(1, NodePluginManager.FUNCTION_TYPE, this);
        featureUpgradeStorage = new EtherPluginUpgradeContainer(6, NodePluginManager.FEATURE_UPGRADE_TYPE, this);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide())
            markUpdate = true;
    }

    @Override
    public void tickServer() {
        functionStorage.tick();
        featureUpgradeStorage.tick();
        if (markUpdate) {
            markUpdate = false;
            updateProperty();
        }
    }

    private void updateProperty() {
        nodeProperty.reset();
        functionStorage.modifyNodeProperty(nodeProperty);
        featureUpgradeStorage.modifyNodeProperty(nodeProperty);
    }

    @Override
    public long getMaxEther() {
        return nodeProperty.maxEther;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        functionStorage.loadAddition(input.childOrEmpty("functionStorage"));
        featureUpgradeStorage.loadAddition(input.childOrEmpty("featureUpgradeStorage"));
        input.read("content", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l ->
                ContainerOps.fillContainerByItemList(normalStorage, l));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        functionStorage.saveAddition(output.child("functionStorage"));
        featureUpgradeStorage.saveAddition(output.child("featureUpgradeStorage"));
        output.store("content", ItemStack.OPTIONAL_CODEC.listOf(), ContainerOps.containerToItemList(normalStorage));
    }

    @Override
    public int size() {
        return nodeProperty.slotUnlock + 1;
    }

    @Override
    public @NotNull ItemResource getResource(int index) {
        if (index == 0)
            return etherStorage.getResource(index);
        return normalHandler.getResource(index - 1);
    }

    @Override
    public long getAmountAsLong(int index) {
        if (index == 0)
            return etherStorage.getAmountAsLong(index);
        return normalHandler.getAmountAsLong(index - 1);
    }

    @Override
    public long getCapacityAsLong(int index, @NotNull ItemResource resource) {
        if (index == 0)
            return etherStorage.getCapacityAsLong(index, resource);
        return normalHandler.getCapacityAsLong(index - 1, resource);
    }

    @Override
    public boolean isValid(int index, @NotNull ItemResource resource) {
        if (index == 0)
            return etherStorage.isValid(index, resource);
        if (resource.is(ItemRegistry.ETHER))
            return false;
        if (index - 1 >= nodeProperty.slotUnlock)
            return false;
        return normalHandler.isValid(index - 1, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
        if (index == 0)
            return etherStorage.insert(index, resource, amount, transaction);
        if (resource.is(ItemRegistry.ETHER))
            return 0;
        if (index - 1 >= nodeProperty.slotUnlock)
            return 0;
        return normalHandler.insert(index - 1, resource, amount, transaction);
    }

    @Override
    public int extract(int index, @NotNull ItemResource resource, int amount, TransactionContext transaction) {
        if (index == 0)
            return etherStorage.extract(index, resource, amount, transaction);
        if (resource.is(ItemRegistry.ETHER))
            return 0;
        if (index - 1 >= nodeProperty.slotUnlock)
            return 0;
        return normalHandler.extract(index - 1, resource, amount, transaction);
    }
    public ItemStack extractWithPredicate(Predicate<ItemResource> predicate, TransactionContext transaction, int maxAmount) {
        for (int i = 0; i < normalHandler.size(); i++) {
            ItemResource resource = normalHandler.getResource(i);
            if (predicate.test(resource)) {
                int extract = normalHandler.extract(i, resource, Math.min(resource.getMaxStackSize(), maxAmount), transaction);
                return resource.toStack(extract);
            }
        }
        return ItemStack.EMPTY;
    }

    public List<Pair<NodePluginManager.PluginInfo, InstalledPlugin>> getTabProvider() {
        List<Pair<NodePluginManager.PluginInfo, InstalledPlugin>> list = new ArrayList<>();
        list.add(new Pair<>(NodePluginManager.MAIN_PAGE_INFO, NodePluginManager.MAIN_PAGE));
        for (int i = 0; i < functionStorage.getContainerSize(); i++) {
            ItemStack stack = functionStorage.getItem(i);
            NodePluginManager.PluginInfo info = NodePluginManager.Instance.getInfoFor(stack, NodePluginManager.FUNCTION_TYPE);
            if (!stack.isEmpty() && info != null && info.type() == NodePluginManager.PluginType.FUNCTION) {
                list.add(new Pair<>(info, new InstalledPlugin(info.type(), i, info.id())));
            }
        }
        for (int i = 0; i < featureUpgradeStorage.getContainerSize(); i++) {
            ItemStack stack = featureUpgradeStorage.getItem(i);
            if (!stack.isEmpty()) {
                NodePluginManager.PluginInfo info = NodePluginManager.Instance.getInfoFor(stack, NodePluginManager.FEATURE_UPGRADE_TYPE);
                if (info != null && info.type() == NodePluginManager.PluginType.FEATURE) {
                    list.add(new Pair<>(info, new InstalledPlugin(info.type(), i, info.id())));
                }
            }
        }
        return list;
    }

    public AbstractNodePlugin getOrCreatePluginForMenu(@NotNull InstalledPlugin plugin) {
        if (plugin.type() == NodePluginManager.PluginType.FUNCTION)
            if (functionStorage.hasPlugin(plugin.id()))
                return functionStorage.getPlugin(plugin.id());
        if (plugin.type() == NodePluginManager.PluginType.FEATURE || plugin.type() == NodePluginManager.PluginType.UPGRADE)
            if (featureUpgradeStorage.hasPlugin(plugin.id()))
                return featureUpgradeStorage.getPlugin(plugin.id());
        return NodePluginManager.Instance.get(plugin.pluginId(), this);
    }

    public int getUpgradeCount() {
        int level = getBlockState().getValueOrElse(EtherAdaptNodeBlock.LEVEL, 1);
        return Config.nodeLevelSlotArr.get(level - 1);
    }

    public MenuProvider getMenuProvider(@Nullable InstalledPlugin installedPlugin) {
        return new MenuProvider() {
            @Override
            public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                return false;
            }

            @Override
            public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
                buffer.writeBlockPos(getBlockPos());
                InstalledPlugin.writeNullable(installedPlugin, buffer);
            }

            @Override
            public Component getDisplayName() {
                return Component.empty();
            }

            @Override
            public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                return new EtherAdaptNodeContainerMenu(i, player, getBlockPos(), installedPlugin);
            }
        };
    }

}
