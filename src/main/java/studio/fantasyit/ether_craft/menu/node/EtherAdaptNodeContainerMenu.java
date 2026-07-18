package studio.fantasyit.ether_craft.menu.node;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.block.node.EtherSlotSyncContainer;
import studio.fantasyit.ether_craft.menu.base.BaseContainerMenu;
import studio.fantasyit.ether_craft.menu.base.BaseMenu;
import studio.fantasyit.ether_craft.menu.base.IFilterSwitchable;
import studio.fantasyit.ether_craft.menu.base.ether.EtherContainerSyncer;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.base.slot.FilterSlot;
import studio.fantasyit.ether_craft.menu.factory.slot.SingleStackSlot;
import studio.fantasyit.ether_craft.menu.node.slot.OversizedEtherSlot;
import studio.fantasyit.ether_craft.network.base.ISyncTargetMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.network.s2c.SyncEtherAdaptNodeExtraS2C;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.MainPageDummyPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static studio.fantasyit.ether_craft.register.GuiRegistry.ETHER_ADAPT_NODE_CONTAINER;

public class EtherAdaptNodeContainerMenu extends BaseMenu<EtherAdaptNodeEntity> implements ISyncTargetMenu, IFilterSwitchable {
    public final Player player;
    public final AbstractNodePlugin plugin;
    public final InstalledPlugin installedPlugin;
    public final List<Slot> toDrawSlot = new ArrayList<>();
    private final EtherContainerSyncer syncer;
    public PluginMenuContext context;
    public int machineSlotStart = -1;

    public static EtherAdaptNodeContainerMenu readFromNetwork(int windowId, Player player, RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        InstalledPlugin installedPlugin = InstalledPlugin.readNullable(data);
        if(!(player.level().getBlockEntity(pos) instanceof EtherAdaptNodeEntity)) {
            return null;
        }
        return new EtherAdaptNodeContainerMenu(windowId, player, pos, installedPlugin);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            if (slotIndex < machineSlotStart) {
                if (!this.moveItemStackTo(stack, machineSlotStart, machineSlotStart + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean isEtherItem = stack.is(ItemRegistry.ETHER) || stack.is(ItemRegistry.ETHER_CREATIVE);
                boolean hasToggle = plugin instanceof IFilterSwitchable;
                boolean skipNormalSlots = hasToggle && isFilterActive();
                for (int i = 0; i < machineSlotStart && !stack.isEmpty(); i++) {
                    Slot target = this.slots.get(i);
                    if (target instanceof SingleStackSlot) continue;
                    if (isEtherItem) {
                        if (target instanceof OversizedEtherSlot && target.container instanceof EtherSlotSyncContainer essc) {
                            essc.insertItemStack(stack);
                        }
                    } else {
                        if (target instanceof OversizedEtherSlot) continue;
                        if (target instanceof FilterSlot fs && isFilterActive()) {
                            if (!fs.hasItem() && !fs.handler.hasAnyMatching(s -> ItemStack.isSameItemSameComponents(s, stack)))
                                fs.set(stack.copyWithCount(1));
                        } else if (target.mayPlace(stack) && !skipNormalSlots && target.isActive())
                            this.moveItemStackTo(stack, i, i + 1, false);
                    }
                }
                if (!stack.isEmpty() && context instanceof MainPageDummyPlugin.MainPageContext mpc) {
                    if (!mpc.functionStorage.hasItem()) {
                        Identifier matchingPluginId = NodePluginManager.Instance.getMatchingPluginId(NodePluginManager.FUNCTION_TYPE, stack);
                        if (matchingPluginId != null) {
                            this.moveItemStackTo(stack, mpc.functionStorage.index, mpc.functionStorage.index + 1, false);
                        }
                    }
                    for (int i = 0; i < mpc.normalStorage.size() && !stack.isEmpty(); i++) {
                        Slot target = mpc.normalStorage.get(i);
                        if (!target.hasItem()) {
                            Identifier matchingPluginId = NodePluginManager.Instance.getMatchingPluginId(NodePluginManager.FEATURE_UPGRADE_TYPE, stack);
                            if (matchingPluginId != null) {
                                this.moveItemStackTo(stack, target.index, target.index + 1, false);
                            }
                        }
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return !entity.isRemoved();
    }

    public EtherAdaptNodeContainerMenu(int windowId, Player player, BlockPos pos) {
        this(windowId, player, pos, null);
    }

    public EtherAdaptNodeContainerMenu(int windowId, Player player, BlockPos pos, @Nullable InstalledPlugin plugin) {
        super(windowId, player, pos, ETHER_ADAPT_NODE_CONTAINER.get());
        this.player = player;
        if (plugin == null)
            plugin = NodePluginManager.MAIN_PAGE;
        this.installedPlugin = plugin;
        this.plugin = entity.getOrCreatePluginForMenu(plugin);
        if (this.plugin != null) {
            this.context = this.plugin.makeContext(this);
        }
        addMachineSlots();
        addPlayerSlots(player.getInventory());
        addDataSlot(new BaseDataSlot(
                () -> entity.nodeProperty.enableFilter ? 1 : 0,
                val -> entity.nodeProperty.enableFilter = (val == 1)
        ));
        addDataSlot(new BaseDataSlot(
                () -> isFilterActive() ? 1 : 0,
                val -> setFilterActive(val == 1)
        ));
        entity.syncClient();
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new SyncEtherAdaptNodeExtraS2C(
                    Optional.ofNullable(entity.functionPlugin),
                    entity.featureAttachedDirection,
                    entity.syncedPluginData,
                    entity.getBlockPos(),
                    entity.getLevel().dimension().identifier(),
                    entity.nodeProperty.maxEther,
                    entity.nodeProperty.slotUnlock
            ));
        }
        this.syncer = new EtherContainerSyncer(this.entity, this::addDataSlot);
    }

    @Override
    protected void addMachineSlots() {
    }

    @Override
    protected void addPlayerSlots(Inventory playerInventory) {
        int invBaseX = 8;
        int invBaseY = 146;
        addSlotArea(playerInventory, 9, invBaseX, invBaseY, 9, 18, 3, 18, BaseContainerMenu.SlotSupplier.of(Slot::new));
        addSlotArea(playerInventory, 0, invBaseX, invBaseY + 58, 9, 18, 1, 18, BaseContainerMenu.SlotSupplier.of(Slot::new));
    }

    @Override
    public @NotNull Slot addSlot(Slot slot) {
        if (machineSlotStart < 0 && slot.container instanceof Inventory) {
            machineSlotStart = this.slots.size();
        }
        return super.addSlot(slot);
    }

    public @NotNull Slot addSlotDraw(Slot slot) {
        toDrawSlot.add(slot);
        return super.addSlot(slot);
    }

    @Override
    public DataSlot addDataSlot(DataSlot dataSlot) {
        return super.addDataSlot(dataSlot);
    }

    @Override
    public int addSlotArea(Container container, int startIdx, int x, int y, int slotPreRow, int dx, int slotPreCol, int dy, BaseContainerMenu.SlotSupplier slotSupplier) {
        return super.addSlotArea(container, startIdx, x, y, slotPreRow, dx, slotPreCol, dy, slotSupplier);
    }

    public int addSlotAreaDraw(Container container, int startIdx, int x, int y, int slotPreRow, int dx, int slotPreCol, int dy, BaseContainerMenu.SlotSupplier slotSupplier) {
        return super.addSlotArea(container, startIdx, x, y, slotPreRow, dx, slotPreCol, dy, slotSupplier, (tt, a, b) -> toDrawSlot.add(tt));
    }


    @Override
    public int addSlotArea(Container container, int startIdx, int x, int y, int slotPreRow, int dx, int slotPreCol, int dy, BaseContainerMenu.SlotSupplier slotSupplier, @Nullable TriConsumer<Slot, Integer, Integer> slotConsumer) {
        return super.addSlotArea(container, startIdx, x, y, slotPreRow, dx, slotPreCol, dy, slotSupplier, slotConsumer);
    }

    public void triggerSwitchTabServer(InstalledPlugin plugin) {
        player.openMenu(entity.getMenuProvider(plugin));
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        InstalledPlugin target = message.plugin();
        AbstractNodePlugin plugin;
        if (target.type() == NodePluginManager.PluginType.FUNCTION) {
            plugin = entity.functionStorage.getPlugin(target.id());
        } else {
            plugin = entity.featureUpgradeStorage.getPlugin(target.id());
        }
        if (plugin != null) {
            plugin.syncScreenData(message);
        }
    }

    @Override
    public boolean isFilterActive() {
        if (context instanceof IFilterSwitchable s) return s.isFilterActive();
        return true;
    }

    @Override
    public void setFilterActive(boolean active) {
        if (context instanceof IFilterSwitchable s) s.setFilterActive(active);
    }
}
