package studio.fantasyit.ether_craft.node.tip;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.*;

public class NodePluginTipManager {
    public static final NodePluginTipManager INSTANCE = new NodePluginTipManager();

    private final Map<Identifier, TipInfo> tips = new HashMap<>();

    public void collect() {
        tips.clear();
    }

    public void registerTip(Identifier pluginId, TipInfo tipInfo) {
        tips.put(pluginId, tipInfo);
    }

    public Optional<TipInfo> getTip(Identifier pluginId) {
        return Optional.ofNullable(tips.get(pluginId));
    }

    public boolean hasTip(Identifier pluginId) {
        return tips.containsKey(pluginId);
    }

    public void setClientTips(Map<Identifier, TipInfo> data) {
        tips.clear();
        tips.putAll(data);
    }

    public Map<Identifier, TipInfo> getAllTips() {
        return Map.copyOf(tips);
    }
}
