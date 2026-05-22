package studio.fantasyit.ether_craft.node.plugins.feature;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

public abstract class AbstractDirectionalFeature extends AbstractNodePlugin {
    public static final Identifier SYNC_DIRECTION = EtherCraft.id("directional_feature/direction");
    public @Nullable Direction direction;

    public AbstractDirectionalFeature(EtherAdaptNodeEntity nodeEntity, InstalledPlugin ID) {
        super(nodeEntity, ID);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.storeNullable("direction", Direction.CODEC, direction);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        direction = input.read("direction", Direction.CODEC).orElse(null);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        if (message.id().equals(SYNC_DIRECTION) && message.index() == installedId.id()) {
            if (message.data() == -1)
                direction = null;
            else
                direction = Direction.values()[message.data()];
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> direction == null ? -1 : direction.ordinal(), t -> direction = (t == -1 ? null : Direction.values()[t])));
    }
}
