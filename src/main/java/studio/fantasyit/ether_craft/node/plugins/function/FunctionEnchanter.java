package studio.fantasyit.ether_craft.node.plugins.function;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.menu.node.slot.OversizedEtherSlot;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegCommon;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

import java.util.List;
import java.util.Optional;

public class FunctionEnchanter extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("enchanter");
    public static final Identifier SYNC_LEVEL = EtherCraft.id("enchanter_level");
    public int selectedLevel = 1;
    public int progress = 0;

    public final SimpleContainer processSlot = new SimpleContainer(1);
    public ItemFilter filter = new ItemFilter(21, nodeEntity::setChanged);

    public FunctionEnchanter(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {
        if (nodeProperty.slotUnlock == 0)
            nodeProperty.slotUnlock = 1;
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        menu.addSlotDraw(new OversizedEtherSlot(nodeEntity.etherStorage, 0, 28, 20));
        menu.addSlotDraw(new Slot(processSlot, 0, 28, 44));
        menu.addDataSlot(new BaseDataSlot(() -> selectedLevel, (i) -> selectedLevel = i));
        menu.addDataSlot(new BaseDataSlot(() -> progress, (i) -> progress = i));
        FilterGuiRegCommon.slots(menu, filter);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        super.syncScreenData(message);
        FilterGuiRegCommon.sync(message, filter);
        if (message.id().equals(SYNC_LEVEL)) {
            int level = message.data();
            if (level == -1) {
                selectedLevel = -1;
                return;
            }
            if (level >= 0 && Config.nodeEnchanterEtherCosts.size() > level) {
                int etherCost = Config.nodeEnchanterEtherCosts.get(level);
                if (nodeEntity.getMaxEther() >= etherCost) {
                    selectedLevel = level;
                }
            }
        }
    }

    @Override
    public void tickWork() {
        if (nodeEntity.getLevel() == null || nodeEntity.getLevel().isClientSide())
            return;

        int etherCost = selectedLevel == -1 ? 0 : Config.nodeEnchanterEtherCosts.get(selectedLevel);
        if (progress > 0) {
            if (nodeEntity.getEther() < etherCost) {
                progress = 0;
                return;
            }
            if (processSlot.getItem(0).isEmpty()) {
                progress = 0;
                return;
            }
            progress++;
            if (progress == Config.nodeEnchanterMaxProgress) {
                performEnchantment();
            }
            if (progress >= Config.nodeEnchanterMaxProgress) {
                tryPlaceToMain();
            }
            return;
        }

        if (processSlot.getItem(0).isEmpty()) {
            try (Transaction tx = Transaction.openRoot()) {
                ItemStack extracted = nodeEntity.extractExactWithPredicate(
                        r -> isEnchantableAndUnenchanted(r) && filter.accepts(r), tx, 1
                );
                if (!extracted.isEmpty()) {
                    tx.commit();
                    processSlot.setItem(0, extracted);
                }
            }
        }

        if (!processSlot.getItem(0).isEmpty() && nodeEntity.getEther() >= etherCost) {
            if (isEnchantableAndUnenchanted(ItemResource.of(processSlot.getItem(0)))) {
                if (selectedLevel != -1)
                    progress = 1;
            } else {
                tryPlaceToMain();
            }
        }
    }

    private boolean isEnchantableAndUnenchanted(ItemResource resource) {
        if (resource.isEmpty()) return false;
        ItemStack stack = resource.toStack();
        if (!stack.has(DataComponents.ENCHANTABLE))
            return false;
        if (stack.has(DataComponents.ENCHANTMENTS) && !stack.get(DataComponents.ENCHANTMENTS).isEmpty())
            return false;
        if (stack.has(DataComponents.STORED_ENCHANTMENTS) && !stack.get(DataComponents.STORED_ENCHANTMENTS).isEmpty())
            return false;
        return true;
    }

    private void performEnchantment() {
        ItemStack itemStack = processSlot.getItem(0);
        if (itemStack.isEmpty()) {
            return;
        }

        ItemStack result = makeResultAndCost(itemStack);
        processSlot.setItem(0, result);
    }

    private void tryPlaceToMain() {
        ItemStack result = processSlot.getItem(0);
        if (result.isEmpty()) {
            progress = 0;
            return;
        }
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = nodeEntity.insert(ItemResource.of(result), result.getCount(), tx);
            if (inserted > 0) {
                tx.commit();
                processSlot.setItem(0, ItemStack.EMPTY);
                progress = 0;
            }
        }
    }

    private ItemStack makeResultAndCost(ItemStack itemStack) {
        ServerLevel level = (ServerLevel) nodeEntity.getLevel();
        RandomSource random = level.getRandom();
        int qualityCost = EnchantmentHelper.getEnchantmentCost(random, selectedLevel, 15, itemStack);

        if (qualityCost <= 0) {
            return itemStack;
        }

        RegistryAccess registryAccess = level.registryAccess();
        Optional<HolderSet.Named<Enchantment>> tagOpt = registryAccess
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(EnchantmentTags.IN_ENCHANTING_TABLE);

        if (tagOpt.isEmpty()) {
            return itemStack;
        }

        List<EnchantmentInstance> enchants = EnchantmentHelper.selectEnchantment(
                random, itemStack.copy(), qualityCost, tagOpt.get().stream()
        );

        if (enchants.isEmpty()) {
            return itemStack;
        }

        ItemStack result = itemStack.copy();
        for (EnchantmentInstance ench : enchants) {
            result.enchant(ench.enchantment(), ench.level());
        }
        nodeEntity.extractEther(Config.nodeEnchanterEtherCosts.get(selectedLevel));
        return result;
    }


    @Override
    public void saveAdditional(ValueOutput output) {
        output.putInt("selectedLevel", selectedLevel);
        output.putInt("progress", progress);
        output.store("processSlot", ItemStack.OPTIONAL_CODEC, processSlot.getItem(0));
        filter.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        selectedLevel = input.read("selectedLevel", Codec.INT).orElse(0);
        progress = input.read("progress", Codec.INT).orElse(0);
        processSlot.setItem(0, input.read("processSlot", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        filter.deserialize(input);
    }
}
