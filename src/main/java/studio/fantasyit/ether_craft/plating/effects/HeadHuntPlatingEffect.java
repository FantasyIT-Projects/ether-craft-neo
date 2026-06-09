package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingKillTrigger;

import java.util.Map;

public class HeadHuntPlatingEffect implements IPlatingKillTrigger {

//    private static final Map<EntityType<?>, ItemStack> HEAD_MAP = Map.ofEntries(
//            Map.entry(EntityType.SKELETON, new ItemStack(Items.SKELETON_SKULL)),
//            Map.entry(EntityType.ZOMBIE, new ItemStack(Items.ZOMBIE_HEAD)),
//            Map.entry(EntityType.CREEPER, new ItemStack(Items.CREEPER_HEAD)),
//            Map.entry(EntityType.WITHER_SKELETON, new ItemStack(Items.WITHER_SKELETON_SKULL)),
//            Map.entry(EntityType.PIGLIN, new ItemStack(Items.PIGLIN_HEAD)),
//            Map.entry(EntityType.ENDER_DRAGON, new ItemStack(Items.DRAGON_HEAD))
//    );

    @Override
    public double getEffectByEther(long ether) {
        return Math.sqrt(ether) / 10.0;
    }

    @Override
    public void onKill(PlatingData data, ItemStack stack, Player player, LivingEntity target, LivingDropsEvent event) {
//        if (!PlatingUtil.canExtractEther(stack, Config.platingHeadHuntEtherPerKill)) return;
//
//        double chance = data.effect();
//        if (player.getRandom().nextDouble() >= chance) return;
//
//        ItemStack head = HEAD_MAP.get(target.getType());
//        if (head == null || head.isEmpty()) return;
//
//        Level level = target.level();
//        ItemEntity headEntity = new ItemEntity(level, target.getX(), target.getY(), target.getZ(), head.copy());
//        event.getDrops().add(headEntity);
//        PlatingUtil.extractEther(stack, Config.platingHeadHuntEtherPerKill);
    }
}
