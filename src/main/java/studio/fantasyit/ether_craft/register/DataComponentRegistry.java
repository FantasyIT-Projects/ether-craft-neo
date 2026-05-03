package studio.fantasyit.ether_craft.register;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;

public class DataComponentRegistry {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT = DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, EtherCraft.MODID);
    public static final DeferredHolder<DataComponentType<?>, @NotNull DataComponentType<Identifier>> CHIP_ID = DATA_COMPONENT.register("ether_process_chip_id",
            () -> DataComponentType.<Identifier>builder().persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).build()
    );

    public static void register(IEventBus modbus) {
        DATA_COMPONENT.register(modbus);
    }
}