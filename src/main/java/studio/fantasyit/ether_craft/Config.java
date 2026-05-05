package studio.fantasyit.ether_craft;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = EtherCraft.MODID)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue ETHER_CONVERT = BUILDER
            .comment("How many ether value to gain from one ether item")
            .defineInRange("ether.convert", 100, 1, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int etherConvert;
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        etherConvert = ETHER_CONVERT.get();
    }
}
