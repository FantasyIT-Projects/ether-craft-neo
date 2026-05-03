package studio.fantasyit.ether_craft.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;

public class BaseIOBlockEntity extends BlockEntity implements ResourceHandler<@NotNull ItemResource> {
    public final SimpleContainer inputContainer;
    public final SimpleContainer internalContainer;
    public final SimpleContainer outputContainer;
    public final CompoundContainer container;
    public final ResourceHandler<@NotNull ItemResource> handler;
    public final int input;
    public final int internal;
    public final int output;
    public BaseIOBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState,int input,int internal,int outputs) {
        super(type, worldPosition, blockState);
        this.input = input;
        this.internal = internal;
        this.output = outputs;
        inputContainer = new SimpleContainer(input);
        internalContainer = new SimpleContainer(internal);
        outputContainer = new SimpleContainer(outputs);
        container = new CompoundContainer(inputContainer, new CompoundContainer(internalContainer, outputContainer));
        handler = VanillaContainerWrapper.of(container);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        inputContainer.fromItemList(input.listOrEmpty("all", ItemStack.OPTIONAL_CODEC));
        internalContainer.fromItemList(input.listOrEmpty("all", ItemStack.OPTIONAL_CODEC));
        outputContainer.fromItemList(input.listOrEmpty("all", ItemStack.OPTIONAL_CODEC));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        inputContainer.storeAsItemList(output.list("input",ItemStack.OPTIONAL_CODEC));
        internalContainer.storeAsItemList(output.list("internal",ItemStack.OPTIONAL_CODEC));
        outputContainer.storeAsItemList(output.list("output",ItemStack.OPTIONAL_CODEC));
    }

    @Override
    public int size() {
        return handler.size();
    }

    @Override
    public ItemResource getResource(int index) {
        return handler.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return handler.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return handler.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return handler.isValid(index, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
        return handler.insert(resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
        return handler.extract(resource, amount, transaction);
    }

    public enum SLOT_TYPE {
        ETHER,
        INPUT,
        OUTPUT,
        UNKNOWN
    }
    public interface CallWithSlot<T> {
        T call(Container handler, int slot, SLOT_TYPE type);
    }

    public interface CallWithSlotV {
        void call(Container handler, int slot, SLOT_TYPE type);
    }
    public  <T> T redirectSlot(int slot, CallWithSlot<T> callWithSlot) {
        return redirectSlot(slot, callWithSlot, true);
    }

    public  <T> T redirectSlot(int slot, CallWithSlot<T> callWithSlot, boolean ignoreUnknown) {
        if (slot < input) {
            return callWithSlot.call(inputContainer, slot, SLOT_TYPE.INPUT);
        } else if (slot < input + internal) {
            return callWithSlot.call(internalContainer, slot - input, SLOT_TYPE.ETHER);
        } else if (slot < input + internal + output) {
            return callWithSlot.call(outputContainer, slot - input - internal, SLOT_TYPE.OUTPUT);
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
