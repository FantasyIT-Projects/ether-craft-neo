package studio.fantasyit.ether_craft.stream.cap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

import java.util.ArrayList;
import java.util.List;

public class EtherStreamDamageCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("damage_dealer");

    public static final Codec<EtherStreamDamageCapability> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("weapons").forGetter(c -> c.weapons)
    ).apply(instance, weapons -> {
        EtherStreamDamageCapability cap = new EtherStreamDamageCapability();
        cap.weapons.addAll(weapons);
        return cap;
    }));

    private final List<ItemStack> weapons = new ArrayList<>();

    public void addWeapon(ItemStack weapon) {
        weapons.add(weapon.copy());
    }

    public boolean hasWeapons() {
        return !weapons.isEmpty();
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void tick(IEtherStreamLike streamEntity) {
    }

    @Override
    public boolean hitEntity(ServerLevel level, IEtherStreamLike streamEntity, EntityHitResult hit, Entity entity) {
        if (weapons.isEmpty()) return false;

        ItemStack bestWeapon = findBestWeapon();
        if (bestWeapon.isEmpty()) return false;

        Weapon weapon = bestWeapon.get(DataComponents.WEAPON);
        if (weapon == null) return false;

        int damage = weapon.itemDamagePerAttack();
        int cost = Math.max(1, damage * Config.etherStreamDamageEtherMultiplier + Config.etherStreamDamageConstantCost);

        streamEntity.consumeEther(cost);

        Entity sourceEntity = streamEntity instanceof Entity e ? e : null;
        DamageSource source = level.damageSources().indirectMagic(sourceEntity, null);
        entity.hurtServer(level, source, damage);

        return true;
    }

    @Override
    public boolean hitBlock(ServerLevel level, IEtherStreamLike streamEntity, BlockHitResult hit, BlockState blockState) {
        return false;
    }

    @Override
    public void onDestroy(IEtherStreamLike streamEntity) {
    }

    private ItemStack findBestWeapon() {
        ItemStack best = ItemStack.EMPTY;
        int bestDamage = -1;
        for (ItemStack weapon : weapons) {
            if (!weapon.isEmpty() && weapon.has(DataComponents.WEAPON)) {
                Weapon w = weapon.get(DataComponents.WEAPON);
                if (w != null && w.itemDamagePerAttack() > bestDamage) {
                    bestDamage = w.itemDamagePerAttack();
                    best = weapon;
                }
            }
        }
        return best;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("weapons", ItemStack.OPTIONAL_CODEC.listOf(), weapons);
    }

    @Override
    public void deserialize(ValueInput input) {
        weapons.clear();
        input.read("weapons", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(weapons::addAll);
    }
}
