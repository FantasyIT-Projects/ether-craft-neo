package studio.fantasyit.ether_craft.block.emitter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.block.base.BaseEtherContainerBlockEntity;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.block.base.ITickable;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeBlock;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;

import static studio.fantasyit.ether_craft.register.BlockEntityRegistry.ETHER_NODE_ENTITY;

public class EtherStreamEmitterEntity extends BaseEtherContainerBlockEntity implements EtherContainer, MenuProvider, ITickable {
    private boolean markUpdate = false;

    public EtherStreamEmitterEntity(BlockPos worldPosition, BlockState blockState) {
        super(ETHER_NODE_ENTITY.get(), worldPosition, blockState, 9, 9, 0);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide())
            markUpdate = true;
    }

    @Override
    public void tickServer() {
        if (this.getEther() > 1000) {
            if (level != null) {
                NonNullList<ItemStack> copyItem = NonNullList.withSize(this.inputContainer.getContainerSize(), ItemStack.EMPTY);
                for (int i = 0; i < this.inputContainer.getContainerSize(); i++) {
                    copyItem.set(i, this.inputContainer.getItem(i).copy());
                    this.inputContainer.setItem(i, ItemStack.EMPTY);
                }
                @NotNull Direction targetDirection = this.getBlockState().getValue(EtherAdaptNodeBlock.FACING);
                Vec3 dir = targetDirection.getUnitVec3();
                EtherStreamEntity entity = EtherStreamEntity.create(
                        this.level,
                        copyItem,
                        copyItem.size(),
                        (int) this.getEther(),
                        this.getBlockPos().getCenter().add(dir),
                        dir.multiply(0.1f, 0.1f, 0.1f)
                );

                this.setEther(0);
                level.addFreshEntity(entity);
            }
        }
        if (markUpdate) {
            markUpdate = false;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    }

    @Override
    public Component getDisplayName() {
        return Component.empty();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return null;
    }
}
