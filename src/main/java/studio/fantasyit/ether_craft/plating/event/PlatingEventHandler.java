package studio.fantasyit.ether_craft.plating.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.CamouflageState;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.TrackingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingEventHelper;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.*;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class PlatingEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, entity) -> {
            if (effect instanceof IPlatingTickEquippedTrigger holdTick) {
                holdTick.onHoldTick(data, stack, player);
            }
        });

        CamouflageState state = player.getExistingData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get()).orElse(null);
        if (state != null && state.isActive()) {
            boolean hasPlating = false;
            for (ItemStack s : PlatingEventHelper.getPlatedEquipment(player)) {
                for (PlatingData d : PlatingUtil.getPlatingData(s)) {
                    if (d.id().equals(EtherCraft.id("camouflage"))) {
                        hasPlating = true;
                        break;
                    }
                }
                if (hasPlating) break;
            }
            if (!hasPlating) {
                player.setInvisible(false);
                player.setData(AttachmentDataRegistry.CAMOUFLAGE_STATE.get(), CamouflageState.INACTIVE);
            } else {
                player.setInvisible(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        for (ItemStack stack : PlatingEventHelper.getPlatedEquipment(player)) {
            PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
                if (effect instanceof IPlatingRightClickTrigger rt && rt.onRightClick(data, s, entity)) {
                    event.setCanceled(true);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        ItemStack stack = event.getItem();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingUseTrigger use) {
                use.onUse(data, s, entity);
            }
        });
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingAttackTrigger attack && attack.onAttack(data, s, entity, event.getTarget())) {
                event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent
    public static void onBreakBlock(BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        ItemStack stack = player.getMainHandItem();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingBreakBlockTrigger breakBlock) {
                if (breakBlock.onBreakBlock(data, s, entity, event.getPos(), event.getState())) {
                    event.setCanceled(true);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onUseOnBlock(UseItemOnBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack stack = event.getItemStack();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingUseOnBlockTrigger useOnBlock) {
                @Nullable InteractionResult result = useOnBlock.onUseOnBlock(data, s, entity, event.getPos(), event.getLevel().getBlockState(event.getPos()));
                if (result != null) {
                    event.cancelWithResult(result);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onArrowShot(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel)) return;

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem)) {
            held = player.getOffhandItem();
            if (!(held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem)) return;
        }

        PlatingEventHelper.forEachPlating(held, player, (effect, data, stack, entity) -> {
            if (effect instanceof IPlatingArrowShotTrigger arrowShot) {
                arrowShot.onArrowShot(data, stack, player, arrow);
            }
        });
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        ItemStack stack = player.getMainHandItem();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingCritTrigger trigger) {
                trigger.onCriticalHit(data, stack, player, event);
            }
        });
        if (event.getDamageMultiplier() <= 1) {
            event.setDamageMultiplier(1.5f);
        }
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, entity) -> {
            if (effect instanceof IPlatingCritDamageModifier modifier) {
                modifier.onCriticalHit(data, stack, player, event);
            }
        });

    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, entity) -> {
            if (effect instanceof IPlatingKillTrigger kill) {
                kill.onKill(data, stack, player, event.getEntity(), event);
            }
        });
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player)) return;
        if (event.getLevel().isClientSide()) return;

        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, entity) -> {
            if (effect instanceof IPlatingBlockDropsTrigger trigger) {
                trigger.onBlockDrops(data, stack, player, event);
            }
        });
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (arrow.level().isClientSide()) return;

        TrackingData tracking = arrow.getExistingData(AttachmentDataRegistry.ARROW_TRACKING.get()).orElse(null);
        if (tracking == null || tracking.range() <= 0) return;

        if (!(arrow.getOwner() instanceof LivingEntity owner)) return;

        Level level = arrow.level();
        Vec3 arrowPos = arrow.position();
        LivingEntity nearest = null;
        double nearestDist = tracking.range();

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                arrow.getBoundingBox().inflate(tracking.range()))) {
            if (entity == owner) continue;
            if (!entity.isAlive()) continue;
            double dist = entity.distanceToSqr(arrow);
            if (dist < nearestDist * nearestDist) {
                nearest = entity;
                nearestDist = Math.sqrt(dist);
            }
        }

        if (nearest == null) return;

        Vec3 toTarget = nearest.getEyePosition().subtract(arrowPos).normalize();
        Vec3 currentVel = arrow.getDeltaMovement();
        double speed = currentVel.length();
        Vec3 newVel = currentVel.add(toTarget.scale(tracking.strength())).normalize().scale(speed);
        arrow.setDeltaMovement(newVel);
    }

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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityShieldBlock(LivingShieldBlockEvent event) {
        if (event.getBlocked()) {
            ItemStack stack = event.getEntity().getUseItem();
            if (stack.isEmpty()) return;
            if (stack.has(DataComponentRegistry.TEMP_BLOCKING))
                PlatingEventHelper.forEachPlating(stack, event.getEntity(), (effect, data, s, entity) -> {
                    if (effect instanceof IPlatingBlockingTrigger blockingTrigger)
                        blockingTrigger.blocked(data, entity, s, event.getDamageContainer());
                });
        }
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
