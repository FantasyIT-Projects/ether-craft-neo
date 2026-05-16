package studio.fantasyit.ether_craft.block.node;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.neoforge.network.PacketDistributor;
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
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.menu.base.RangeLimitPlaceContainer;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.s2c.SyncEtherAdaptNodeExtraS2C;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.feature.AbstractDirectionalFeature;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.util.SerializeUtil;

import java.util.*;
import java.util.function.Predicate;

import static studio.fantasyit.ether_craft.register.BlockEntityRegistry.ETHER_NODE_ENTITY;

public class EtherAdaptNodeEntity extends BlockEntity implements ResourceHandler<@NotNull ItemResource>, EtherContainer, ITickable {
    private final ResourceHandler<ItemResource> normalHandler;
    private boolean markUpdate = true;
    public final NodeProperty nodeProperty;
    public final EtherSlotSyncContainer etherStorage;
    public final EtherPluginUpgradeContainer functionStorage;
    public final EtherPluginUpgradeContainer featureUpgradeStorage;
    public final RangeLimitPlaceContainer normalStorage;
    public final ItemFilter normalStorageFilter;
    public @Nullable InstalledPlugin functionPlugin;
    public final Map<Direction, InstalledPlugin> featureAttachedDirection = new HashMap<>();
    public final Map<InstalledPlugin, Map<Identifier, Integer>> syncedPluginData = new HashMap<>();
    public final QueuedTicket ticket = new QueuedTicket();
    public String name = "";


    public EtherAdaptNodeEntity(BlockPos worldPosition, BlockState blockState) {
        super(ETHER_NODE_ENTITY.get(), worldPosition, blockState);
        nodeProperty = new NodeProperty();
        etherStorage = new EtherSlotSyncContainer(this);
        normalStorage = new RangeLimitPlaceContainer(new SimpleContainer(27), 0);
        normalStorageFilter = new ItemFilter(27, this::setChanged);
        normalHandler = VanillaContainerWrapper.of(normalStorage);
        functionStorage = new EtherPluginUpgradeContainer(1, NodePluginManager.FUNCTION_TYPE, this);
        featureUpgradeStorage = new EtherPluginUpgradeContainer(6, NodePluginManager.FEATURE_UPGRADE_TYPE, this);
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    public void updatePluginInfos() {
        featureAttachedDirection.clear();
        for (int i = 0; i < featureUpgradeStorage.getContainerSize(); i++) {
            if (featureUpgradeStorage.hasPlugin(i)) {
                if (featureUpgradeStorage.getPlugin(i) instanceof AbstractDirectionalFeature df && df.direction != null) {
                    featureAttachedDirection.put(df.direction, df.installedId);
                }
            }
        }
        if (functionStorage.hasPlugin(0))
            functionPlugin = Objects.requireNonNull(functionStorage.getPlugin(0)).installedId;
        else
            functionPlugin = null;
        Direction functionDirection = getBlockState().getValueOrElse(EtherAdaptNodeBlock.FACING, Direction.NORTH);
        if (featureAttachedDirection.containsKey(functionDirection)) {
            for (Direction d : Direction.values()) {
                if (!featureAttachedDirection.containsKey(d)) {
                    getBlockState().setValue(EtherAdaptNodeBlock.FACING, d);
                    break;
                }
            }
        }
        if (level instanceof ServerLevel sl)
            PacketDistributor.sendToPlayersInDimension(sl, new SyncEtherAdaptNodeExtraS2C(
                    Optional.ofNullable(functionPlugin),
                    featureAttachedDirection,
                    syncedPluginData,
                    this.getBlockPos(),
                    this.level.dimension().identifier(),
                    nodeProperty.maxEther
            ));
    }

    @Override
    public void tickServer() {
        functionStorage.tick();
        featureUpgradeStorage.tick();
        ticket.tick(this);
        if (markUpdate) {
            markUpdate = false;
            updateProperty();
            updatePluginInfos();
        }
    }

    private void updateProperty() {
        nodeProperty.reset();
        functionStorage.modifyNodeProperty(nodeProperty);
        featureUpgradeStorage.modifyNodeProperty(nodeProperty);
        normalStorage.setAccessibleCount(nodeProperty.slotUnlock);
    }

    @Override
    public long getMaxEther() {
        return nodeProperty.maxEther;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read("name", Codec.STRING).ifPresent(n -> name = n);
        functionStorage.loadAddition(input.childOrEmpty("functionStorage"));
        featureUpgradeStorage.loadAddition(input.childOrEmpty("featureUpgradeStorage"));
        normalStorage.deserialize(input.childOrEmpty("normalStorage"));
        normalStorageFilter.deserialize(input.childOrEmpty("normalStorageFilter"));
        input.read("sync", SerializeUtil.PIMap.CODEC.listOf().xmap(
                SerializeUtil.PIMap::toMap,
                SerializeUtil.PIMap::fromMap
        )).ifPresent(m -> {
            syncedPluginData.clear();
            syncedPluginData.putAll(m);
        });
        pluginUpdate();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("name", Codec.STRING, name);
        functionStorage.saveAddition(output.child("functionStorage"));
        featureUpgradeStorage.saveAddition(output.child("featureUpgradeStorage"));
        normalStorage.serialize(output.child("normalStorage"));
        normalStorageFilter.serialize(output.child("normalStorageFilter"));
        output.store("sync", SerializeUtil.PIMap.CODEC.listOf(), SerializeUtil.PIMap.fromMap(syncedPluginData));
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
        if (nodeProperty.enableFilter) {
            ItemStack filterStack = normalStorageFilter.getItem(index - 1);
            if (!filterStack.isEmpty() && !resource.is(filterStack.getItem()))
                return false;
        }
        for (AbstractNodePlugin plugin : getPlugins()) {
            if (!plugin.inputFilter(resource))
                return false;
        }
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

        for (AbstractNodePlugin plugin : getPlugins()) {
            amount -= plugin.earlyHandleInput(resource, amount, transaction);
        }
        for (AbstractNodePlugin plugin : getPlugins()) {
            if (!plugin.inputFilter(resource))
                return 0;
        }
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
        for (AbstractNodePlugin plugin : getPlugins()) {
            if (!plugin.outputFilter(resource))
                return 0;
        }
        return normalHandler.extract(index - 1, resource, amount, transaction);
    }

    public ItemStack extractWithPredicate(Predicate<ItemResource> predicate, TransactionContext transaction, int maxAmount) {
        for (int i = 0; i < normalHandler.size(); i++) {
            ItemResource resource = normalHandler.getResource(i);
            if (resource.isEmpty())
                continue;
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
        return NodePluginManager.Instance.get(plugin.pluginId(), this, plugin);
    }

    public int getUpgradeCount() {
        int level = getBlockState().getValueOrElse(EtherAdaptNodeBlock.LEVEL, 1);
        return Config.nodeLevelSlotArr.get(level - 1);
    }

    public ItemFilter getNormalStorageFilter() {
        return normalStorageFilter;
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

    public void fromNetwork(Map<Direction, InstalledPlugin> pluginDirection, @Nullable InstalledPlugin functionPlugin, Map<InstalledPlugin, Map<Identifier, Integer>> pluginValue, int maxEther) {
        featureAttachedDirection.clear();
        featureAttachedDirection.putAll(pluginDirection);
        this.functionPlugin = functionPlugin;
        this.syncedPluginData.clear();
        this.syncedPluginData.putAll(pluginValue);
        this.nodeProperty.maxEther = maxEther;
    }

    public boolean isPluginInstalled(InstalledPlugin plugin) {
        if (plugin.type() == NodePluginManager.PluginType.FUNCTION)
            return functionStorage.hasPlugin(plugin.id()) && plugin.pluginId().equals(functionStorage.getPluginId(plugin.id()));
        if (plugin.type() == NodePluginManager.PluginType.FEATURE || plugin.type() == NodePluginManager.PluginType.UPGRADE)
            return featureUpgradeStorage.hasPlugin(plugin.id()) && plugin.pluginId().equals(featureUpgradeStorage.getPluginId(plugin.id()));
        return false;
    }

    public List<AbstractNodePlugin> getPlugins() {
        List<AbstractNodePlugin> list = new ArrayList<>();
        for (int i = 0; i < functionStorage.getContainerSize(); i++) {
            AbstractNodePlugin plugin = functionStorage.getPlugin(i);
            if (plugin != null)
                list.add(plugin);

        }
        for (int i = 0; i < featureUpgradeStorage.getContainerSize(); i++) {
            AbstractNodePlugin plugin = featureUpgradeStorage.getPlugin(i);
            if (plugin != null)
                list.add(plugin);
        }
        return list;
    }

    public ItemStack getItemByInstalled(InstalledPlugin plugin) {
        if (plugin.type() == NodePluginManager.PluginType.FUNCTION)
            return functionStorage.getItem(plugin.id());
        if (plugin.type() == NodePluginManager.PluginType.FEATURE || plugin.type() == NodePluginManager.PluginType.UPGRADE)
            return featureUpgradeStorage.getItem(plugin.id());
        return ItemStack.EMPTY;
    }

    public void rotatePluginsByAxis(Direction.Axis axis) {
        for (AbstractNodePlugin plugin : getPlugins()) {
            plugin.onWrenchRotate(axis);
            if (plugin instanceof AbstractDirectionalFeature directional) {
                if (directional.direction != null) {
                    directional.direction = directional.direction.getCounterClockWise(axis);
                }
            }
        }
        pluginUpdate();
    }

    public void pluginUpdate() {
        setChanged();
        if (level != null && !level.isClientSide())
            markUpdate = true;
    }

    public void setSyncedPluginData(InstalledPlugin plugin, Identifier actionId, int value) {
        syncedPluginData.computeIfAbsent(plugin, _ -> new HashMap<>()).put(actionId, value);
        pluginUpdate();
    }

    public int getSyncedPluginData(InstalledPlugin plugin, Identifier actionId) {
        return syncedPluginData.getOrDefault(plugin, Map.of()).getOrDefault(actionId, 0);
    }
}
