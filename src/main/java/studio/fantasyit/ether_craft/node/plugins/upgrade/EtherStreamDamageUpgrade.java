package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.stream.cap.EtherStreamDamageCapability;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import java.util.Optional;

public class EtherStreamDamageUpgrade extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("damage_upgrade");

    public EtherStreamDamageUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    private ItemStack getWeapon() {
        return nodeEntity.featureUpgradeStorage.getItem(installedId.id());
    }

    @Override
    public void provideCapabilities(IEtherStreamLike entity) {
        ItemStack weapon = getWeapon();
        if (weapon.isEmpty()) return;

        Optional<IStreamCapability> existing = entity.getCapability(EtherStreamDamageCapability.ID);
        if (existing.isPresent() && existing.get() instanceof EtherStreamDamageCapability damageCap) {
            damageCap.addWeapon(weapon);
        } else {
            EtherStreamDamageCapability cap = new EtherStreamDamageCapability();
            cap.addWeapon(weapon);
            entity.addCapability(cap);
        }
    }
}
