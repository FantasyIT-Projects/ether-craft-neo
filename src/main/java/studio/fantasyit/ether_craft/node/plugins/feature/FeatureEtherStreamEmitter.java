package studio.fantasyit.ether_craft.node.plugins.feature;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.stream.EtherStreamStorageCapability;

import java.util.Optional;

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
            long sendWith = Math.min(Integer.MAX_VALUE, nodeEntity.getEther());
            nodeEntity.extractEther(sendWith);
            Vec3 dir = direction.getUnitVec3();
            EtherStreamEntity entity = EtherStreamEntity.create(
                    nodeEntity.getLevel(),
                    (int) sendWith,
                    nodeEntity.getBlockPos().getCenter().add(dir),
                    dir.multiply(0.1f, 0.1f, 0.1f)
            );

            for (int i = 0; i < nodeEntity.featureUpgradeStorage.getContainerSize(); i++) {
                AbstractNodePlugin plugin = nodeEntity.featureUpgradeStorage.getPlugin(i);
                if (plugin instanceof IEtherStreamCapabilityProviderPlugin provider) {
                    provider.provideCapabilities(entity);
                }
            }

            Optional<studio.fantasyit.ether_craft.stream.IStreamCapability> optCap = entity.getCapability(EtherStreamStorageCapability.ID);
            if (optCap.isPresent() && optCap.get() instanceof EtherStreamStorageCapability itemCapability) {
                try (Transaction transaction = Transaction.openRoot()) {
                    for (int i = 0; i < itemCapability.getContainerSize(); i++) {
                        ItemStack itemStack = nodeEntity.extractWithPredicate(filter::accepts, transaction, Integer.MAX_VALUE);
                        if (itemStack.isEmpty()) {
                            break;
                        }
                        itemCapability.setItem(i, itemStack);
                    }
                    transaction.commit();
                }
            }

            if (nodeEntity.getLevel() != null) {
                nodeEntity.getLevel().addFreshEntity(entity);
            }
            return true;
        }
        return false;
    }
}
