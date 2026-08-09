package studio.fantasyit.ether_craft.block.node;

import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EtherPluginUpgradeContainer extends SimpleContainer {
    private final Identifier[] pluginId;
    private final AbstractNodePlugin[] plugin;
    private final Predicate<NodePluginManager.PluginType> type;
    private final EtherAdaptNodeEntity entity;

    private IPreTickPlugin[] preTickPlugins = new IPreTickPlugin[0];
    private ITickInputPlugin[] tickInputPlugins = new ITickInputPlugin[0];
    private ITickWorkPlugin[] tickWorkPlugins = new ITickWorkPlugin[0];
    private ITickOutputPlugin[] tickOutputPlugins = new ITickOutputPlugin[0];
    private IModifyNodePropertyPlugin[] modifyNodePropertyPlugins = new IModifyNodePropertyPlugin[0];
    private IEarlyHandleInputPlugin[] earlyHandleInputPlugins = new IEarlyHandleInputPlugin[0];
    private IOverflowHandlerPlugin[] overflowHandlerPlugins = new IOverflowHandlerPlugin[0];
    private IOnDestroyPlugin[] onDestroyPlugins = new IOnDestroyPlugin[0];
    private IOnBlockUpdatePlugin[] onBlockUpdatePlugins = new IOnBlockUpdatePlugin[0];
    private IShouldSyncEtherPlugin[] shouldSyncEtherPlugins = new IShouldSyncEtherPlugin[0];
    private IOnWrenchRotatePlugin[] onWrenchRotatePlugins = new IOnWrenchRotatePlugin[0];
    private ISaveAdditionalPlugin[] saveAdditionalPlugins = new ISaveAdditionalPlugin[0];
    private ILoadAdditionalPlugin[] loadAdditionalPlugins = new ILoadAdditionalPlugin[0];
    private IRegisterSlotsPlugin[] registerSlotsPlugins = new IRegisterSlotsPlugin[0];
    private ISyncScreenDataPlugin[] syncScreenDataPlugins = new ISyncScreenDataPlugin[0];

    public EtherPluginUpgradeContainer(int size, Predicate<NodePluginManager.PluginType> typePredicate, EtherAdaptNodeEntity etherAdaptNodeEntity) {
        super(size);
        pluginId = new Identifier[size];
        plugin = new AbstractNodePlugin[size];
        this.type = typePredicate;
        this.entity = etherAdaptNodeEntity;
    }

    public boolean hasPlugin(int index) {
        return pluginId[index] != null && plugin[index] != null;
    }

    public @Nullable AbstractNodePlugin getPlugin(int index) {
        return plugin[index];
    }

    public @Nullable Identifier getPluginId(int index) {
        return pluginId[index];
    }

    public IEarlyHandleInputPlugin[] earlyHandleInputPlugins() {
        return earlyHandleInputPlugins;
    }

    public IOverflowHandlerPlugin[] overflowHandlerPlugins() {
        return overflowHandlerPlugins;
    }

    public IOnBlockUpdatePlugin[] onBlockUpdatePlugins() {
        return onBlockUpdatePlugins;
    }

    public IShouldSyncEtherPlugin[] shouldSyncEtherPlugins() {
        return shouldSyncEtherPlugins;
    }

    public IOnWrenchRotatePlugin[] onWrenchRotatePlugins() {
        return onWrenchRotatePlugins;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        for (int i = 0; i < plugin.length; i++) {
            if (!NodePluginManager.Instance.matches(this.type, getItem(i), pluginId[i])) {
                if (plugin[i] instanceof IOnDestroyPlugin onDestroy)
                    onDestroy.onDestroy();
                plugin[i] = null;
                pluginId[i] = NodePluginManager.Instance.getMatchingPluginId(this.type, getItem(i));
                if (pluginId[i] != null) {
                    NodePluginManager.PluginInfo info = NodePluginManager.Instance.getInfoFor(getItem(i), this.type);
                    if (info != null)
                        plugin[i] = NodePluginManager.Instance.get(pluginId[i], this.entity, new InstalledPlugin(info.type(), i, pluginId[i]));
                }
            }
        }
        rebuildPluginArrays();
        this.entity.pluginUpdate();
    }

    private void rebuildPluginArrays() {
        List<IPreTickPlugin> preTick = new ArrayList<>();
        List<ITickInputPlugin> tickInput = new ArrayList<>();
        List<ITickWorkPlugin> tickWork = new ArrayList<>();
        List<ITickOutputPlugin> tickOutput = new ArrayList<>();
        List<IModifyNodePropertyPlugin> modifyNodeProperty = new ArrayList<>();
        List<IEarlyHandleInputPlugin> earlyHandleInput = new ArrayList<>();
        List<IOverflowHandlerPlugin> overflowHandler = new ArrayList<>();
        List<IOnDestroyPlugin> onDestroy = new ArrayList<>();
        List<IOnBlockUpdatePlugin> onBlockUpdate = new ArrayList<>();
        List<IShouldSyncEtherPlugin> shouldSyncEther = new ArrayList<>();
        List<IOnWrenchRotatePlugin> onWrenchRotate = new ArrayList<>();
        List<ISaveAdditionalPlugin> saveAdditional = new ArrayList<>();
        List<ILoadAdditionalPlugin> loadAdditional = new ArrayList<>();
        List<IRegisterSlotsPlugin> registerSlots = new ArrayList<>();
        List<ISyncScreenDataPlugin> syncScreenData = new ArrayList<>();
        for (AbstractNodePlugin p : plugin) {
            if (p == null)
                continue;
            if (p instanceof IPreTickPlugin t) preTick.add(t);
            if (p instanceof ITickInputPlugin t) tickInput.add(t);
            if (p instanceof ITickWorkPlugin t) tickWork.add(t);
            if (p instanceof ITickOutputPlugin t) tickOutput.add(t);
            if (p instanceof IModifyNodePropertyPlugin t) modifyNodeProperty.add(t);
            if (p instanceof IEarlyHandleInputPlugin t) earlyHandleInput.add(t);
            if (p instanceof IOverflowHandlerPlugin t) overflowHandler.add(t);
            if (p instanceof IOnDestroyPlugin t) onDestroy.add(t);
            if (p instanceof IOnBlockUpdatePlugin t) onBlockUpdate.add(t);
            if (p instanceof IShouldSyncEtherPlugin t) shouldSyncEther.add(t);
            if (p instanceof IOnWrenchRotatePlugin t) onWrenchRotate.add(t);
            if (p instanceof ISaveAdditionalPlugin t) saveAdditional.add(t);
            if (p instanceof ILoadAdditionalPlugin t) loadAdditional.add(t);
            if (p instanceof IRegisterSlotsPlugin t) registerSlots.add(t);
            if (p instanceof ISyncScreenDataPlugin t) syncScreenData.add(t);
        }
        preTickPlugins = preTick.toArray(new IPreTickPlugin[0]);
        tickInputPlugins = tickInput.toArray(new ITickInputPlugin[0]);
        tickWorkPlugins = tickWork.toArray(new ITickWorkPlugin[0]);
        tickOutputPlugins = tickOutput.toArray(new ITickOutputPlugin[0]);
        modifyNodePropertyPlugins = modifyNodeProperty.toArray(new IModifyNodePropertyPlugin[0]);
        earlyHandleInputPlugins = earlyHandleInput.toArray(new IEarlyHandleInputPlugin[0]);
        overflowHandlerPlugins = overflowHandler.toArray(new IOverflowHandlerPlugin[0]);
        onDestroyPlugins = onDestroy.toArray(new IOnDestroyPlugin[0]);
        onBlockUpdatePlugins = onBlockUpdate.toArray(new IOnBlockUpdatePlugin[0]);
        shouldSyncEtherPlugins = shouldSyncEther.toArray(new IShouldSyncEtherPlugin[0]);
        onWrenchRotatePlugins = onWrenchRotate.toArray(new IOnWrenchRotatePlugin[0]);
        saveAdditionalPlugins = saveAdditional.toArray(new ISaveAdditionalPlugin[0]);
        loadAdditionalPlugins = loadAdditional.toArray(new ILoadAdditionalPlugin[0]);
        registerSlotsPlugins = registerSlots.toArray(new IRegisterSlotsPlugin[0]);
        syncScreenDataPlugins = syncScreenData.toArray(new ISyncScreenDataPlugin[0]);
    }

    public static Identifier ID_NULL = EtherCraft.id("null");

    public void saveAddition(ValueOutput output) {
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), getItems());
        output.store("plugins", Identifier.CODEC.listOf(), Stream.of(pluginId).map(id -> id == null ? ID_NULL : id).toList());
        for (ISaveAdditionalPlugin save : saveAdditionalPlugins)
            save.saveAdditional(output.child(String.format("plugin-%d", ((AbstractNodePlugin) save).installedId.id())));
    }

    public void loadAddition(ValueInput input) {
        input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(l -> {
            for (int i = 0; i < l.size() && i < getContainerSize(); i++) {
                setItem(i, l.get(i), true);
            }
        });
        input.read("plugins", Identifier.CODEC.listOf()).ifPresent(l -> {
            for (int i = 0; i < l.size() && i < getContainerSize(); i++) {
                pluginId[i] = l.get(i);
                if (pluginId[i].equals(ID_NULL)) {
                    plugin[i] = null;
                }
                if (pluginId[i] != null) {
                    NodePluginManager.PluginInfo info = NodePluginManager.Instance.getInfoFor(getItem(i), this.type);
                    if (info != null)
                        plugin[i] = NodePluginManager.Instance.get(pluginId[i], this.entity, new InstalledPlugin(info.type(), i, pluginId[i]));
                }
            }
        });
        rebuildPluginArrays();
        for (ILoadAdditionalPlugin load : loadAdditionalPlugins)
            load.loadAdditional(input.childOrEmpty(String.format("plugin-%d", ((AbstractNodePlugin) load).installedId.id())));
        setChanged();
    }

    public void tickInput() {
        for (ITickInputPlugin p : tickInputPlugins)
            p.tickInput();
    }

    public void tickWork() {
        for (ITickWorkPlugin p : tickWorkPlugins)
            p.tickWork();
    }

    public void tickOutput() {
        for (ITickOutputPlugin p : tickOutputPlugins)
            p.tickOutput();
    }

    public void modifyNodeProperty(NodeProperty nodeProperty) {
        for (IModifyNodePropertyPlugin p : modifyNodePropertyPlugins)
            p.modifyNodeProperty(nodeProperty);
    }

    public boolean preTick() {
        for (IPreTickPlugin p : preTickPlugins) {
            if (!p.preTick())
                return false;
        }
        return true;
    }

    public int handleOverflow(ItemStack stack, int amount, TransactionContext transaction) {
        int consumed = 0;
        for (IOverflowHandlerPlugin p : overflowHandlerPlugins)
            consumed += p.handleOverflow(stack, amount - consumed, transaction);
        return consumed;
    }
}
