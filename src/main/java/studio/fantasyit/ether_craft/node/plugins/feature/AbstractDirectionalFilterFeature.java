package studio.fantasyit.ether_craft.node.plugins.feature;

import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.filter.ItemFilter;

public abstract class AbstractDirectionalFilterFeature extends AbstractNodePlugin {
    public @Nullable Direction direction;
    public ItemFilter filter = new ItemFilter(21, nodeEntity::setChanged);

    public AbstractDirectionalFilterFeature(EtherAdaptNodeEntity nodeEntity) {
        super(nodeEntity);
    }

    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {
    }

    @Override
    public abstract void tick();

    @Override
    public void saveAdditional(ValueOutput output) {
        if (direction != null)
            output.store("direction", Direction.CODEC, direction);
        filter.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        direction = input.read("direction", Direction.CODEC).orElse(null);
        filter.deserialize(input);
    }
}
