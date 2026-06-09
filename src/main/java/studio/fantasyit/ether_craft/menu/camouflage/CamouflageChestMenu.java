package studio.fantasyit.ether_craft.menu.camouflage;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.CamouflageState;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.register.GuiRegistry;

public class CamouflageChestMenu extends ChestMenu {
    private static final int TARGET_ROWS = 3;
    private static final int VIEWER_ROWS = 3;
    private static final int SLOTS_PER_ROW = 9;

    private final Inventory targetInv;
    private final Player targetPlayer;

    public CamouflageChestMenu(int containerId, Inventory viewerInv, Player targetPlayer) {
        super(GuiRegistry.CAMOUFLAGE_CHEST.get(), containerId);
        this.targetPlayer = targetPlayer;
        this.targetInv = targetPlayer.getInventory();
        addSlots(viewerInv);
    }

    public CamouflageChestMenu(int containerId, Inventory viewerInv, RegistryFriendlyByteBuf data) {
        super(GuiRegistry.CAMOUFLAGE_CHEST.get(), containerId);
        int entityId = data.readVarInt();
        if (viewerInv.player.level().isClientSide()) {
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            this.targetPlayer = entity instanceof Player p ? p : viewerInv.player;
        } else {
            this.targetPlayer = viewerInv.player;
        }
        this.targetInv = this.targetPlayer.getInventory();
        addSlots(viewerInv);
    }

    private void addSlots(Inventory viewerInv) {
        for (int row = 0; row < TARGET_ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int targetSlotIndex = 9 + row * SLOTS_PER_ROW + col;
                this.addSlot(new Slot(targetInv, targetSlotIndex, 8 + col * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < VIEWER_ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                this.addSlot(new Slot(viewerInv, 9 + row * SLOTS_PER_ROW + col, 8 + col * 18, 85 + row * 18));
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        int targetStart = 0;
        int targetEnd = TARGET_ROWS * SLOTS_PER_ROW;
        int viewerStart = targetEnd;
        int viewerEnd = viewerStart + VIEWER_ROWS * SLOTS_PER_ROW;

        if (index < targetEnd) {
            if (!this.moveItemStackTo(stack, viewerStart, viewerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, targetStart, targetEnd, true)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (targetPlayer == null || targetPlayer.isRemoved()) return false;
        if (targetPlayer.distanceToSqr(player) > 64.0) return false;
        CamouflageState state = targetPlayer.getExistingData(
                AttachmentDataRegistry.CAMOUFLAGE_STATE.get()).orElse(null);
        return state != null && state.isActive();
    }
}
