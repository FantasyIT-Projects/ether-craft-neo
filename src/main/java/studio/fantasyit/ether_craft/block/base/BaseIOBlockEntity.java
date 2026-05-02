package studio.fantasyit.ether_craft.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

public class BaseIOBlockEntity extends BlockEntity implements ResourceHandler<@NotNull ItemResource> {
    public final ItemStacksResourceHandler inputHandler;
    public final ItemStacksResourceHandler internalHandler;
    public final ItemStacksResourceHandler outputHandler;
    public final int input;
    public final int internal;
    public final int output;
    public BaseIOBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState,int input,int internal,int outputs) {
        super(type, worldPosition, blockState);
        this.input = input;
        this.internal = internal;
        this.output = outputs;
        inputHandler = new ItemStacksResourceHandler(input);
        internalHandler = new ItemStacksResourceHandler(internal);
        outputHandler = new ItemStacksResourceHandler(outputs);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        inputHandler.deserialize(input.childOrEmpty("input"));
        internalHandler.deserialize(input.childOrEmpty("internal"));
        outputHandler.deserialize(input.childOrEmpty("output"));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        inputHandler.serialize(output.child("input"));
        internalHandler.serialize(output.child("internal"));
        outputHandler.serialize(output.child("output"));
    }

    @Override
    public int size() {
        return input + internal + output;
    }

    @Override
    public ItemResource getResource(int index) {
        return redirectSlot(index, (handler, slot, type) -> handler.getResource(slot), false);
    }

    @Override
    public long getAmountAsLong(int index) {
        return redirectSlot(index, (handler, slot, type) -> handler.getAmountAsLong(slot), false);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return redirectSlot(index, (handler, slot, type) -> handler.getCapacityAsLong(slot, resource), false);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return redirectSlot(index, (handler, slot, type) -> handler.isValid(slot, resource), false);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
        return redirectSlot(index, (handler, slot, type) -> handler.insert(slot, resource, amount, transaction), false);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
        return redirectSlot(index, (handler, slot, type) -> handler.extract(slot, resource, amount, transaction), false);
    }

    public enum SLOT_TYPE {
        ETHER,
        INPUT,
        OUTPUT,
        UNKNOWN
    }
    public interface CallWithSlot<T> {
        T call(ItemStacksResourceHandler handler, int slot, SLOT_TYPE type);
    }

    public interface CallWithSlotV {
        void call(ItemStacksResourceHandler handler, int slot, SLOT_TYPE type);
    }
    public  <T> T redirectSlot(int slot, CallWithSlot<T> callWithSlot) {
        return redirectSlot(slot, callWithSlot, true);
    }

    public  <T> T redirectSlot(int slot, CallWithSlot<T> callWithSlot, boolean ignoreUnknown) {
        if (slot < input) {
            return callWithSlot.call(inputHandler, slot, SLOT_TYPE.INPUT);
        } else if (slot < input + internal) {
            return callWithSlot.call(internalHandler, slot - input, SLOT_TYPE.ETHER);
        } else if (slot < input + internal + output) {
            return callWithSlot.call(outputHandler, slot - input - internal, SLOT_TYPE.OUTPUT);
        } else if (!ignoreUnknown) {
            return callWithSlot.call(null, slot, SLOT_TYPE.UNKNOWN);
        }
        return null;
    }

    public void redirectSlotV(int slot, CallWithSlotV callWithSlot) {
        redirectSlot(slot, (a, b, c) -> {
            callWithSlot.call(a, b, c);
            return false;
        });
    }

}
