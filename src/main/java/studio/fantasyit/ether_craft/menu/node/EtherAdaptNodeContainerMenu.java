package studio.fantasyit.ether_craft.menu.node;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.BaseMenu;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.BaseContainerMenu;
import studio.fantasyit.ether_craft.menu.base.BaseDataSlot;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.register.Tags;

import java.util.ArrayList;
import java.util.List;

import static studio.fantasyit.ether_craft.register.GuiRegistry.ETHER_ADAPT_NODE_CONTAINER;

public class EtherAdaptNodeContainerMenu extends BaseMenu<EtherAdaptNodeEntity> {
    public final Player player;
    public final AbstractNodePlugin plugin;
    public final InstalledPlugin installedPlugin;
    public final List<Slot> toDrawSlot = new ArrayList<>();

    public static EtherAdaptNodeContainerMenu readFromNetwork(int windowId, Player player, RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        InstalledPlugin installedPlugin = InstalledPlugin.readNullable(data);
        return new EtherAdaptNodeContainerMenu(windowId, player, pos, installedPlugin);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;//TODO
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
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
        if (this.plugin != null)
            this.plugin.registerSlots(this);
        addMachineSlots();
        addPlayerSlots(player.getInventory());
        entity.syncClient();
    }

    @Override
    protected void addMachineSlots() {    }

    @Override
    protected void addPlayerSlots(Inventory playerInventory) {
        int invBaseX = 8;
        int invBaseY = 146;
        addSlotArea(playerInventory, 9, invBaseX, invBaseY, 9, 18, 3, 18, BaseContainerMenu.SlotSupplier.of(Slot::new));
        addSlotArea(playerInventory, 0, invBaseX, invBaseY + 58, 9, 18, 1, 18, BaseContainerMenu.SlotSupplier.of(Slot::new));
    }

    @Override
    public @NotNull Slot addSlot(Slot slot) {
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
}
