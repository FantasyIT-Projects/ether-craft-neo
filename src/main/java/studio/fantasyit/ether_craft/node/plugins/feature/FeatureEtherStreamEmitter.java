package studio.fantasyit.ether_craft.node.plugins.feature;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public class FeatureEtherStreamEmitter extends AbstractDirectionalFilterFeature {
    public static final Identifier ID = EtherCraft.id("ether_stream_emitter");

    public FeatureEtherStreamEmitter(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    public void tick() {
        queueWithCd(ID, 10, this::process);
    }

    private boolean process() {
        if (direction != null && nodeEntity.getEther() > 1000) {
            long sendWith = nodeEntity.getEther();
            nodeEntity.setEther(0);
            NonNullList<ItemStack> itemList = NonNullList.create();
            try (Transaction transaction = Transaction.openRoot()) {
                for (int i = 0; i < nodeEntity.nodeProperty.streamMaxStorage; i++) {
                    ItemStack itemStack = nodeEntity.extractWithPredicate(filter::accepts, transaction, Integer.MAX_VALUE);
                    itemList.add(itemStack);
                    if (itemStack.isEmpty()) {
                        break;
                    }
                }
                transaction.commit();
            }
            Vec3 dir = direction.getUnitVec3();
            EtherStreamEntity entity = EtherStreamEntity.create(
                    nodeEntity.getLevel(),
                    itemList,
                    nodeEntity.nodeProperty.streamMaxStorage,
                    (int) sendWith,
                    nodeEntity.getBlockPos().getCenter().add(dir),
                    dir.multiply(0.1f, 0.1f, 0.1f)
            );
            if (nodeEntity.getLevel() != null) {
                nodeEntity.getLevel().addFreshEntity(entity);
            }
            return true;
        }
        return false;
    }
}
