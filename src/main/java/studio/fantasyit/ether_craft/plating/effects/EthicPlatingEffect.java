package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingAttackTrigger;

import java.util.Optional;

public class EthicPlatingEffect implements IPlatingEffect, IPlatingAttackTrigger {
    public static final Identifier ID = EtherCraft.id("ethic");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (!(entity instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!PlatingUtil.canExtractEther(stack, Config.platingEthicEtherPerUse)) return;

        if (target instanceof IronGolem golem) {
            PlatingUtil.extractEther(stack, Config.platingEthicEtherPerUse);
            golem.setTarget(null);
            event.setCanceled(true);
            return;
        }

        if (target instanceof TamableAnimal tamable) {
            PlatingUtil.extractEther(stack, Config.platingEthicEtherPerUse);
            tamable.setTarget(null);
            event.setCanceled(true);
            return;
        }

        if (target instanceof Animal animal) {
            Optional<ResourceKey<LootTable>> lootTableKey = animal.getLootTable();
            if (lootTableKey.isEmpty()) {
                event.setCanceled(true);
                return;
            }
            PlatingUtil.extractEther(stack, Config.platingEthicEtherPerUse);
            LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableKey.get());
            LootParams lootParams = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, animal)
                    .withParameter(LootContextParams.ORIGIN, animal.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, player.damageSources().playerAttack(player))
                    .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, player)
                    .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, player)
                    .create(LootContextParamSets.ENTITY);
            lootTable.getRandomItems(lootParams, animal.getLootTableSeed(), itemStack -> animal.spawnAtLocation(level, itemStack));
            animal.setLastHurtByMob(null);
            animal.setTarget(null);
            event.setCanceled(true);
        }
    }
}

