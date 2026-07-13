package studio.fantasyit.ether_craft.node.plugins.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public class FeatureDropperThrower extends AbstractDirectionalFilterFeature {
    public static final Identifier ID = EtherCraft.id("dropper_thrower");
    public static final Identifier SYNC_THROW_COUNT = EtherCraft.id("dropper_thrower/throw_count");

    public int throwCount = 1;

    public FeatureDropperThrower(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    public void tickOutput() {
        if (direction != null)
            queueWithCd(ID, 1, this::process);
    }

    private boolean process() {
        if (direction == null) return true;
        if (nodeEntity.getLevel() == null) return true;

        ItemStack itemStack;
        long etherCost;
        try (Transaction transaction = Transaction.openRoot()) {
            itemStack = nodeEntity.extractExactWithPredicate(filter::accepts, transaction, throwCount);
            if (itemStack.isEmpty()) {
                return true;
            }
            etherCost = (long) itemStack.getCount() * Config.nodeDropperThrowerEtherPerItem;
            if (nodeEntity.getEther() < etherCost) {
                return false;
            }
            nodeEntity.extractEther(etherCost);
            transaction.commit();
        }

        Direction dir = direction;
        Vec3 dirVec = dir.getUnitVec3();
        Vec3 spawnPos = nodeEntity.getBlockPos().getCenter().add(
                dirVec.x * 0.8,
                dirVec.y * 0.8,
                dirVec.z * 0.8
        );
        double speedX = dirVec.x * 0.3;
        double speedY = dirVec.y * 0.3;
        double speedZ = dirVec.z * 0.3;
        ItemEntity itemEntity = new ItemEntity(
                nodeEntity.getLevel(),
                spawnPos.x, spawnPos.y, spawnPos.z,
                itemStack,
                speedX, speedY, speedZ
        );
        nodeEntity.getLevel().addFreshEntity(itemEntity);
        return true;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("throwCount", Codec.INT, throwCount);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        throwCount = input.read("throwCount", Codec.INT).orElse(1);
        nodeEntity.setSyncedPluginData(installedId, SYNC_THROW_COUNT, throwCount);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        super.syncScreenData(message);
        if (message.id().equals(SYNC_THROW_COUNT)) {
            throwCount = Math.clamp(message.data(), 1, 64);
            nodeEntity.setSyncedPluginData(installedId, SYNC_THROW_COUNT, throwCount);
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> throwCount, t -> throwCount = t));
    }
}
