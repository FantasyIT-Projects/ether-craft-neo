package studio.fantasyit.ether_craft.register;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

import java.util.ArrayList;
import java.util.List;

public class DataComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, EtherCraft.MODID);
    public static final DeferredHolder<DataComponentType<?>, @NotNull DataComponentType<Identifier>> CHIP_ID = DATA_COMPONENT.register("ether_process_chip_id",
            () -> DataComponentType.<Identifier>builder().persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).build()
    );
    public static final DeferredHolder<DataComponentType<?>, @NotNull DataComponentType<Integer>> CONVERSION_COUNTER = DATA_COMPONENT.register("ether_process_chip_level",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build()
    );
    public static final DeferredHolder<DataComponentType<?>, @NotNull DataComponentType<Integer>> DURABILITY = DATA_COMPONENT.register("ether_process_chip_durability",
            () -> DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build()
    );

    public static final DeferredHolder<DataComponentType<?>, @NotNull DataComponentType<List<List<ItemStack>>>> GRID = DATA_COMPONENT.register("ether_process_recipe_grid",
            () -> DataComponentType.<List<List<ItemStack>>>builder().persistent(ItemStack.OPTIONAL_CODEC.listOf().listOf()).networkSynchronized(ByteBufCodecs.collection(
                    ArrayList::new, ByteBufCodecs.collection(ArrayList::new, ItemStack.OPTIONAL_STREAM_CODEC)
            )).build()
    );
    public static final DeferredHolder<DataComponentType<?>, @NotNull DataComponentType<ItemStackTemplate>> TARGET = DATA_COMPONENT.register("ether_process_recipe_target",
            () -> DataComponentType.<ItemStackTemplate>builder().persistent(ItemStackTemplate.CODEC).networkSynchronized(ItemStackTemplate.STREAM_CODEC).build()
    );

    public static void register(IEventBus modbus) {
        DATA_COMPONENT.register(modbus);
    }
}