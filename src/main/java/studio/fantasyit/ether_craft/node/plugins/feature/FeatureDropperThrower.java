package studio.fantasyit.ether_craft.node.plugins.feature;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public class FeatureDropperThrower extends AbstractDirectionalFilterFeature {
    public static final Identifier ID = EtherCraft.id("dropper_thrower");

    public FeatureDropperThrower(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    public void tick() {
        queueWithCd(ID, 1, this::process);
    }

    private boolean process() {

        if (nodeEntity.getEther() < Config.dropperThrowerEtherPreItem)
            return false;
        if (direction == null) {
            return true;
        }
        if (nodeEntity.getLevel() == null) {
            return true;
        }
        ItemStack itemStack;
        try (Transaction transaction = Transaction.openRoot()) {
            itemStack = nodeEntity.extractWithPredicate(filter::accepts, transaction, Integer.MAX_VALUE);
            transaction.commit();
        }
        nodeEntity.extractEther(Config.dropperThrowerEtherPreItem);
        if (itemStack.isEmpty()) {
            return true;
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
}
