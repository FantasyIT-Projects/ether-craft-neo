package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.menu.camouflage.CamouflageChestMenu;
import studio.fantasyit.ether_craft.menu.grid.answer.AnswerFetchMenu;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;

public class GuiRegistry {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, EtherCraft.MODID);
    public static final DeferredHolder<MenuType<?>, @NotNull MenuType<EtherProcessFactoryContainerMenu>> ETHER_PROCESS_FACTORY_CONTAINER = MENU_TYPES.register("ether_process_factory_gui",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new EtherProcessFactoryContainerMenu(windowId, inv.player, data.readBlockPos())));

    public static final DeferredHolder<MenuType<?>, @NotNull MenuType<EtherAdaptNodeContainerMenu>> ETHER_ADAPT_NODE_CONTAINER = MENU_TYPES.register("ether_adapt_node_gui",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> EtherAdaptNodeContainerMenu.readFromNetwork(windowId, inv.player,data)));

    public static final DeferredHolder<MenuType<?>, @NotNull MenuType<AnswerFetchMenu>> ANSWER_FETCH = MENU_TYPES.register("answer_fetch",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new AnswerFetchMenu(windowId, inv, data)));

    public static final DeferredHolder<MenuType<?>, @NotNull MenuType<CamouflageChestMenu>> CAMOUFLAGE_CHEST = MENU_TYPES.register("camouflage_chest",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> new CamouflageChestMenu(windowId, inv, data)));

    public static void init(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}