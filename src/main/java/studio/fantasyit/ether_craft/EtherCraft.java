package studio.fantasyit.ether_craft;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import studio.fantasyit.ether_craft.event.WrenchEventHandler;
import studio.fantasyit.ether_craft.register.*;
import studio.fantasyit.ether_craft.stream.CapabilityFactoryManager;
import studio.fantasyit.ether_craft.stream.cap.*;

@Mod(EtherCraft.MODID)
public class EtherCraft
{
    public static final String MODID = "ether_craft";
    public static final Logger LOGGER = LogUtils.getLogger();
    public EtherCraft(IEventBus modEventBus, ModContainer modContainer)
    {
        WrenchEventHandler.register();
        AttachmentDataRegistry.register(modEventBus);
        BlockEntityRegistry.register(modEventBus);
        BlockRegistry.register(modEventBus);
        GuiRegistry.init(modEventBus);
        ItemRegistry.register(modEventBus);
        RecipeTypeRegistry.register(modEventBus);
        RecipeSerializerRegistry.register(modEventBus);
        CreativeTabRegistry.register(modEventBus);
        DataComponentRegistry.register(modEventBus);
        EntityRegistry.register(modEventBus);
        EntityDataSerializerRegistry.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    public static Identifier id(String path){
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
