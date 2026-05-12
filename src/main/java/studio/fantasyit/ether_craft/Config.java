package studio.fantasyit.ether_craft;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = EtherCraft.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue ETHER_CONVERT = BUILDER
            .comment("How many ether value to gain from one ether item")
            .defineInRange("ether.convert", 100, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue NODE_DEF_MAX_ETHER = BUILDER
            .comment("Max ether value of Ether Adapt Node by default")
            .defineInRange("node.def.max", 1000, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue NODE_MAGNET_CONSUME_PRE_STACK = BUILDER
            .comment("Ether the magnet function will consume when picking up one stack")
            .defineInRange("node.magnet.consume_pre_stack", 100, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue NODE_CONTAINER_INTERACT_ETHER_PRE_ITEM = BUILDER
            .comment("Ether consumed per item transferred by ContainerInteract feature")
            .defineInRange("node.container_interact.ether_pre_item", 100, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> NODE_LEVEL_SLOT_ARR = BUILDER
            .comment("Upgrade slots pre level")
            .defineList("node.up_slot", () -> List.of(2, 4, 6), () -> 0, t -> {
                try {
                    if (t instanceof Integer) return true;
                    Integer.parseInt(t.toString());
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
    static final ModConfigSpec SPEC = BUILDER.build();

    public static int etherConvert;
    public static int nodeDefMaxEther;
    public static List<Integer> nodeLevelSlotArr;
    public static int nodeMagnetConsumePreStack;
    public static int containerInteractEtherPreItem;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        etherConvert = ETHER_CONVERT.get();
        nodeDefMaxEther = NODE_DEF_MAX_ETHER.get();
        nodeLevelSlotArr = NODE_LEVEL_SLOT_ARR.get().stream().map(t -> (Integer) t).toList();
        nodeMagnetConsumePreStack = NODE_MAGNET_CONSUME_PRE_STACK.get();
        containerInteractEtherPreItem = NODE_CONTAINER_INTERACT_ETHER_PRE_ITEM.get();
    }
}
