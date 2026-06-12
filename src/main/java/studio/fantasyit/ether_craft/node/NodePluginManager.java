package studio.fantasyit.ether_craft.node;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.MainPageDummyPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.feature.*;
import studio.fantasyit.ether_craft.node.plugins.function.*;
import studio.fantasyit.ether_craft.node.plugins.upgrade.*;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public class NodePluginManager {

    public enum PluginType implements StringRepresentable {
        FUNCTION("function"),
        FEATURE("feature"),
        UPGRADE("upgrade"),
        DUMMY("dummy");

        public static final Codec<PluginType> CODEC = StringRepresentable.fromEnum(PluginType::values);

        private final String name;

        PluginType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public final static Predicate<PluginType> FUNCTION_TYPE = t -> t == PluginType.FUNCTION;
    public final static Predicate<PluginType> FEATURE_UPGRADE_TYPE = t -> t == PluginType.FEATURE || t == PluginType.UPGRADE;

    public final static InstalledPlugin MAIN_PAGE = new InstalledPlugin(PluginType.DUMMY, 0, MainPageDummyPlugin.ID);
    public final static PluginInfo MAIN_PAGE_INFO;

    public record PluginInfo(PluginType type, Identifier id,
                             BiFunction<EtherAdaptNodeEntity, InstalledPlugin, AbstractNodePlugin> constructor,
                             Predicate<ItemStack> predicate,
                             ItemStackTemplate icon
    ) {
        public PluginInfo(PluginType type, Identifier id,
                          BiFunction<EtherAdaptNodeEntity, InstalledPlugin, AbstractNodePlugin> constructor,
                          Predicate<ItemStack> predicate,
                          Item icon) {
            this(type, id, constructor, predicate, new ItemStackTemplate(icon));
        }
    }

    public static NodePluginManager Instance = new NodePluginManager();
    public List<PluginInfo> plugins;

    public static final List<PluginInfo> ALL_PLUGINS = new ArrayList<>();

    static {
        MAIN_PAGE_INFO = new PluginInfo(PluginType.UPGRADE, MainPageDummyPlugin.ID, MainPageDummyPlugin::new, _ -> false, ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_1.get());
        ALL_PLUGINS.add(MAIN_PAGE_INFO);
        ALL_PLUGINS.add(new PluginInfo(PluginType.FUNCTION, FunctionFurnaceGenerator.ID, FunctionFurnaceGenerator::new, t -> t.is(Items.FURNACE), Items.FURNACE));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FUNCTION, FunctionFurnaceGenerator.ID_BLAST, FunctionFurnaceGenerator::new, t -> t.is(Items.BLAST_FURNACE), Items.BLAST_FURNACE));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FUNCTION, FunctionStoneGenerator.ID, FunctionStoneGenerator::new, t -> t.is(Items.STONECUTTER), Items.STONECUTTER));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FUNCTION, FunctionMagnet.ID, FunctionMagnet::new, t -> t.is(ItemRegistry.VACUUM_PIPE), ItemRegistry.VACUUM_PIPE.get()));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FUNCTION, FunctionNodeProcess.ID, FunctionNodeProcess::new, t -> t.is(Items.CRAFTER), Items.CRAFTER));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FUNCTION, FunctionEquipmentConsumeGenerator.ID, FunctionEquipmentConsumeGenerator::new, t -> t.is(Items.GRINDSTONE), Items.GRINDSTONE));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FUNCTION, FunctionEtherConverter.ID, FunctionEtherConverter::new, t -> t.is(Items.DRAGON_EGG), Items.DRAGON_EGG));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FUNCTION, FunctionGrowthAccelerator.ID, FunctionGrowthAccelerator::new, t -> t.is(Items.BONE_MEAL), Items.BONE_MEAL));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FUNCTION, FunctionEnchanter.ID, FunctionEnchanter::new, t -> t.is(Items.ENCHANTING_TABLE), Items.ENCHANTING_TABLE));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FUNCTION, FunctionCreativeEther.ID_FUNC, FunctionCreativeEther::new, t -> t.is(ItemRegistry.ETHER_CREATIVE), ItemRegistry.ETHER_CREATIVE.get()));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FEATURE, FeatureEtherStreamEmitter.ID, FeatureEtherStreamEmitter::new, t -> t.is(Items.DISPENSER), Items.DISPENSER));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FEATURE, FeatureDropperThrower.ID, FeatureDropperThrower::new, t -> t.is(Items.DROPPER), Items.DROPPER));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FEATURE, FeatureContainerInteract.ID, FeatureContainerInteract::new, t -> t.is(Items.HOPPER), Items.HOPPER));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FEATURE, FeatureRedstoneSignal.ID, FeatureRedstoneSignal::new, t -> t.is(Items.COMPARATOR), Items.COMPARATOR));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FEATURE, DestructionUpgrade.ID, DestructionUpgrade::new, t -> t.is(Items.LAVA_BUCKET), Items.LAVA_BUCKET));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FEATURE, FunctionCreativeEther.ID, FunctionCreativeEther::new, t -> t.is(ItemRegistry.ETHER_CREATIVE), ItemRegistry.ETHER_CREATIVE.get()));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStorageUpgrade.ID, EtherStorageUpgrade::new, t -> t.is(ItemRegistry.ETHERPHILIC_BOWL), ItemRegistry.ETHERPHILIC_BOWL.get()));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, StorageUpgrade.ID, StorageUpgrade::new, t -> t.is(Items.CHEST) || t.is(ItemTags.COPPER_CHESTS), Items.CHEST));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamStorageUpgrade.ID, (a, b) -> new EtherStreamStorageUpgrade(a, b, 1), t -> t.is(ItemTags.CHEST_BOATS), Items.OAK_CHEST_BOAT));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamStorageUpgrade.ID_1, (a, b) -> new EtherStreamStorageUpgrade(a, b, 2), t -> t.is(Items.CHEST_MINECART), Items.CHEST_MINECART));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamStorageUpgrade.ID_2, (a, b) -> new EtherStreamStorageUpgrade(a, b, 4), t -> t.is(ItemTags.SHULKER_BOXES), Items.SHULKER_BOX));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamPreventDecayUpgrade.ID, EtherStreamPreventDecayUpgrade::new, t -> t.is(Items.REPEATER), Items.REPEATER));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamBreakBlockUpgrade.ID, EtherStreamBreakBlockUpgrade::new, t -> t.is(ItemTags.AXES) || t.is(ItemTags.PICKAXES) || t.is(ItemTags.SHOVELS) || t.is(ItemTags.HOES), Items.IRON_PICKAXE));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamDamageUpgrade.ID, EtherStreamDamageUpgrade::new, stack -> stack.has(DataComponents.WEAPON), Items.IRON_SWORD));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherFilterUpgrade.ID, EtherFilterUpgrade::new, t -> t.is(Items.PAPER), Items.PAPER));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherItemifyUpgrade.ID, EtherItemifyUpgrade::new, t -> t.is(ItemRegistry.INACTIVATED_ETHER), ItemRegistry.INACTIVATED_ETHER.get()));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamTextUpgrade.ID, EtherStreamTextUpgrade::new, t -> t.is(Items.WRITTEN_BOOK), Items.WRITTEN_BOOK));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamGrowthAcceleratorUpgrade.ID, EtherStreamGrowthAcceleratorUpgrade::new, t -> t.is(Items.BONE_MEAL), Items.BONE_MEAL));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamGrowthAcceleratorUpgrade.ID_ALL, EtherStreamGrowthAcceleratorUpgrade::new, t -> t.is(Items.SCULK_CATALYST), Items.SCULK_CATALYST));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamCarryEntityUpgrade.ID, EtherStreamCarryEntityUpgrade::new, t -> t.is(ItemTags.BOATS), Items.OAK_BOAT));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamCarryEntityUpgrade.ID_PLAYER, EtherStreamCarryEntityUpgrade::new, t -> t.is(Items.MINECART), Items.MINECART));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamSpeedUpUpgrade.ID, EtherStreamSpeedUpUpgrade::new, t -> t.is(Items.POWERED_RAIL), Items.POWERED_RAIL));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamPlatingUpgrade.ID, EtherStreamPlatingUpgrade::new, t -> t.is(ItemRegistry.PROCESS_CHIP_ITEM) && EtherCraft.id("energizing_chip").equals(t.get(DataComponentRegistry.CHIP_ID)),
                new ItemStackTemplate(ItemRegistry.PROCESS_CHIP_ITEM, DataComponentPatch.builder().set(DataComponentRegistry.CHIP_ID.get(), EtherCraft.id("energizing_chip")).set(DataComponents.ITEM_MODEL, EtherCraft.id("energizing_chip")).build())));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherStreamBounceBackUpgrade.ID, EtherStreamBounceBackUpgrade::new, t -> t.is(Items.SLIME_BALL), Items.SLIME_BALL));
        ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, EtherAutoSupplyUpgrade.ID, EtherAutoSupplyUpgrade::new, t -> t.is(ItemRegistry.ETHER_CRYSTAL.get()), ItemRegistry.ETHER_CRYSTAL.get()));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FEATURE, RedstoneSwitchUpgrade.ID, (a, b) -> new RedstoneSwitchUpgrade(a, b, true), t -> t.is(Items.REDSTONE), Items.REDSTONE));
        ALL_PLUGINS.add(new PluginInfo(PluginType.FEATURE, RedstoneSwitchUpgrade.ID_REVERT, (a, b) -> new RedstoneSwitchUpgrade(a, b, false), t -> t.is(Items.REDSTONE_TORCH), Items.REDSTONE_TORCH));
    }

    public NodePluginManager() {
        plugins = new ArrayList<>();
    }

    public void collect() {
        plugins.clear();
        plugins.addAll(ALL_PLUGINS);
    }

    public boolean matches(Predicate<NodePluginManager.PluginType> type, ItemStack itemStack, @Nullable Identifier identifier) {
        if (identifier == null)
            return itemStack.isEmpty();
        for (PluginInfo info : plugins) {
            if (info.predicate().test(itemStack) && type.test(info.type()))
                return info.id().equals(identifier);
        }
        return false;
    }

    public @Nullable Identifier getMatchingPluginId(Predicate<NodePluginManager.PluginType> type, ItemStack itemStack) {
        for (PluginInfo info : plugins) {
            if (info.predicate().test(itemStack) && type.test(info.type())) {
                return info.id();
            }
        }
        return null;
    }

    public @Nullable AbstractNodePlugin get(Identifier id, EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedPlugin) {
        for (PluginInfo info : plugins) {
            if (info.id().equals(id)) {
                return info.constructor().apply(nodeEntity, installedPlugin);
            }
        }
        return null;
    }

    public PluginInfo getInfoFor(ItemStack stack, Predicate<PluginType> featureUpgradeType) {
        for (PluginInfo info : plugins) {
            if (info.predicate().test(stack) && featureUpgradeType.test(info.type())) {
                return info;
            }
        }
        return null;
    }

    public void registerPlugin(PluginType type, Identifier id, BiFunction<EtherAdaptNodeEntity, InstalledPlugin, AbstractNodePlugin> plugin, Item item) {
        registerPlugin(type, id, plugin, t -> t.is(item.asItem()), item);
    }

    public void registerPlugin(PluginType type, Identifier id, BiFunction<EtherAdaptNodeEntity, InstalledPlugin, AbstractNodePlugin> plugin, Predicate<ItemStack> predicate, Item icon) {
        plugins.add(new PluginInfo(type, id, plugin, predicate, icon));
    }
}
