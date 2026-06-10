package studio.fantasyit.ether_craft.plating.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import studio.fantasyit.ether_craft.plating.data.CamouflageState;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

@EventBusSubscriber
public class CamouflageInteractEventHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()) return;

        if (!(event.getTarget() instanceof Player targetPlayer)) return;
        if (targetPlayer == event.getEntity()) return;

        CamouflageState state = targetPlayer.getExistingData(
                AttachmentDataRegistry.CAMOUFLAGE_STATE.get()).orElse(CamouflageState.INACTIVE);
        if (!state.isActive()) return;

        if (event.getLocalPos().y >= targetPlayer.getEyeHeight() / 2.0) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        Inventory targetInv = targetPlayer.getInventory();
        event.getEntity().openMenu(new SimpleMenuProvider(
                (containerId, inv, player) -> ChestMenu.threeRows(containerId, inv,
                        new BackpackContainer(targetInv)),
                Component.translatable("container.chest")));
    }


    private static class BackpackContainer implements Container {
        private final Inventory playerInv;

        BackpackContainer(Inventory playerInv) {
            this.playerInv = playerInv;
        }

        @Override
        public int getContainerSize() {
            return 27;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < 27; i++) {
                if (!playerInv.getItem(i + 9).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return playerInv.getItem(slot + 9);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return playerInv.removeItem(slot + 9, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return playerInv.removeItemNoUpdate(slot + 9);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            playerInv.setItem(slot + 9, stack);
        }

        @Override
        public void setChanged() {
            playerInv.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return playerInv.stillValid(player);
        }

        @Override
        public void clearContent() {
        }
    }

}
