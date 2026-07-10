package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.fantasyit.ether_craft.EtherCraft;

public class SoundRegistry {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, EtherCraft.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> COYOTE_TIME_LOOP = SOUNDS.register("coyote_time_loop",
            () -> SoundEvent.createVariableRangeEvent(EtherCraft.id("coyote_time_loop")));

    public static void register(IEventBus modbus) {
        SOUNDS.register(modbus);
    }
}