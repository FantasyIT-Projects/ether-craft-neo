package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.integration.Integrations;
import studio.fantasyit.ether_craft.integration.guideme.GuideMeFunctions;
import studio.fantasyit.ether_craft.item.ProcessChipItem;

public class CreativeTabRegistry {
    public static final String TAB_NAME = "ether_craft_tab_main";
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EtherCraft.MODID);
    public static final DeferredHolder<CreativeModeTab, @org.jetbrains.annotations.NotNull CreativeModeTab> CRAFT_TAB =
            CREATIVE_MODE_TABS.register("ether_craft_main", () ->
                    CreativeModeTab.builder().icon(() -> new ItemStack(ItemRegistry.ETHER.get()))
                            .title(Component.translatable(TAB_NAME))
                            .displayItems((pParameter, pOutput) -> {
                                if (Integrations.isGuideMeLoaded())
                                    pOutput.accept(GuideMeFunctions.getGuide());
                                ItemRegistry.ITEMS
                                        .getEntries()
                                        .stream()
                                        .map(DeferredHolder::get)
                                        .filter(item -> !(item instanceof ProcessChipItem) && item != ItemRegistry.LOGO.get())
                                        .forEach(pOutput::accept);
                                EtherProcessChipManager.foreach((id, r) -> {
                                    pOutput.accept(ProcessChipItem.getStackFor(id));
                                });
                            })
                            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
