package studio.fantasyit.ether_craft.node;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.block.node.render.EtherAdapterNodeAtlas;
import studio.fantasyit.ether_craft.block.node.render.EtherAdapterNodeRenderState;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureContainerInteract;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureDropperThrower;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionFurnaceGenerator;
import studio.fantasyit.ether_craft.node.plugins.function.FunctionMagnet;

import java.util.HashMap;
import java.util.Map;

public class PluginRenderManager {
    public interface PluginRender {
        void render(Direction key, Integer dTick, EtherAdaptNodeEntity entity, EtherAdapterNodeRenderState state);
    }

    public static PluginRenderManager Instance = new PluginRenderManager();

    public Map<Identifier, PluginRender> pluginRenderer = new HashMap<>();

    public void collect() {
        register(FunctionFurnaceGenerator.ID, (face, dTick, nodeEntity, state) -> {
            FunctionFurnaceGenerator.WorkingMaterial value = FunctionFurnaceGenerator.WorkingMaterial.values()[nodeEntity.getSyncedPluginData(FunctionFurnaceGenerator.WORKING_MATERIAL)];
            state.setSideAtlas(face, value == FunctionFurnaceGenerator.WorkingMaterial.IDLE ? EtherAdapterNodeAtlas.FUNCTION_BURNER_EMPTY : EtherAdapterNodeAtlas.FUNCTION_BURNER_WORKING);

            if (value == FunctionFurnaceGenerator.WorkingMaterial.COAL) {
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_COAL.get(dTick));
            } else if (value == FunctionFurnaceGenerator.WorkingMaterial.LAVA) {
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_LAVA.get(dTick));
            } else if (value == FunctionFurnaceGenerator.WorkingMaterial.WOOD) {
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_WOOD);
            } else {
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_COAL.get(dTick));
            }

            long maxEther = nodeEntity.getMaxEther();
            if (maxEther != 0)
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_FILL.get((int) Math.min((nodeEntity.getEther() * 10 / maxEther), 9)));
            state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_FILL.get(9));
        });
        register(FeatureContainerInteract.ID, (face, dTick, nodeEntity, state) ->
                state.setSideAtlas(face, switch (face) {
                    case UP -> EtherAdapterNodeAtlas.FEATURE_CONTAINER_INT_TOP;
                    case DOWN -> EtherAdapterNodeAtlas.FEATURE_CONTAINER_INT_BOTTOM;
                    default -> EtherAdapterNodeAtlas.FEATURE_CONTAINER_INT_SIDE;
                }));
        register(FeatureDropperThrower.ID, (face, dTick, nodeEntity, state) ->
                state.setSideAtlas(face, switch (face) {
                    case UP -> EtherAdapterNodeAtlas.FEATURE_DROPPER_TOP;
                    case DOWN -> EtherAdapterNodeAtlas.FEATURE_DROPPER_BOTTOM;
                    default -> EtherAdapterNodeAtlas.FEATURE_DROPPER_SIDE;
                }));
        register(FunctionMagnet.ID, (face, dTick, nodeEntity, state) ->
                state.setSideAtlas(face, switch (face) {
                    case UP -> EtherAdapterNodeAtlas.FEATURE_MAGNET_TOP;
                    case DOWN -> EtherAdapterNodeAtlas.FEATURE_MAGNET_BOTTOM;
                    default -> EtherAdapterNodeAtlas.FEATURE_MAGNET_SIDE;
                }));

    }

    public void register(Identifier id, PluginRender renderer) {
        pluginRenderer.put(id, renderer);
    }

    public void register(Identifier id, EtherAdapterNodeAtlas.AtlasUV renderer) {
        pluginRenderer.put(id, (d, _, _, s) -> s.setSideAtlas(d, renderer));
    }

    public void render(Direction key, InstalledPlugin value, EtherAdaptNodeEntity entity, EtherAdapterNodeRenderState state) {
        LocalPlayer p = Minecraft.getInstance().player;
        int dTick = p == null ? 0 : p.tickCount;
        if (pluginRenderer.containsKey(value.pluginId())) {
            pluginRenderer.get(value.pluginId()).render(key, dTick, entity, state);
        }
    }
}
