package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;

public class GuiRegistry {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, EtherCraft.MODID);
    public static final DeferredHolder<MenuType<?>, @NotNull MenuType<EtherProcessFactoryContainerMenu>> ETHER_PROCESS_FACTORY_CONTAINER = MENU_TYPES.register("ether_process_factory_gui",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new EtherProcessFactoryContainerMenu(windowId, inv.player, data.readBlockPos())));

    public static void init(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}