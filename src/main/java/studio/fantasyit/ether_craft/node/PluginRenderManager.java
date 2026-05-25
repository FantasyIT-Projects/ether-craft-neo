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
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureEtherStreamEmitter;
import studio.fantasyit.ether_craft.node.plugins.function.*;

import java.util.HashMap;
import java.util.Map;

public class PluginRenderManager {
    public interface PluginRender {
        void render(Direction key, Integer dTick, EtherAdaptNodeEntity entity, EtherAdapterNodeRenderState state, InstalledPlugin installedPlugin);
    }

    public static PluginRenderManager Instance = new PluginRenderManager();

    public Map<Identifier, PluginRender> pluginRenderer = new HashMap<>();

    public void collect() {
        PluginRender generatorLayer = (face, dTick, nodeEntity, state, installedPlugin) -> {
            AbstractItemConsumeFunction.WorkingMaterial value = AbstractItemConsumeFunction.WorkingMaterial.values()[nodeEntity.getSyncedPluginData(installedPlugin, AbstractItemConsumeFunction.WORKING_MATERIAL)];
            state.setSideAtlas(face, EtherAdapterNodeAtlas.FUNCTION_BURNER_EMPTY);

            if (value == AbstractItemConsumeFunction.WorkingMaterial.COAL) {
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_COAL.get(dTick));
            } else if (value == AbstractItemConsumeFunction.WorkingMaterial.LAVA) {
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_LAVA.get(dTick));
            } else if (value == AbstractItemConsumeFunction.WorkingMaterial.WOOD) {
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_WOOD);
            } else if (value == AbstractItemConsumeFunction.WorkingMaterial.STONE) {
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_STONE);
            } else if (value == AbstractItemConsumeFunction.WorkingMaterial.DEEPSLATE) {
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_DEEPSLATE);
            } else if (value != AbstractItemConsumeFunction.WorkingMaterial.IDLE) {
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_COAL.get(dTick));
            }

            long maxEther = nodeEntity.getMaxEther();
            if (maxEther != 0)
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_FILL.get((int) Math.min((nodeEntity.getEther() * 10 / maxEther), 10)));
            else
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_FILL.get(10));
        };
        register(FunctionFurnaceGenerator.ID, generatorLayer);
        register(FunctionStoneGenerator.ID, generatorLayer);
        register(FunctionEquipmentConsumeGenerator.ID, generatorLayer);
        register(FunctionEtherConverter.ID, (face, dTick, nodeEntity, state, installedPlugin) -> {
            state.setSideAtlas(face, EtherAdapterNodeAtlas.FUNCTION_ETHER_CONVERTER);
            int workState = nodeEntity.getSyncedPluginData(installedPlugin, FunctionEtherConverter.STATE);
            if (workState == 1) {
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_ETHER_CONVERTER_WORKING.get(dTick));
            }
        });
        register(FunctionEquipmentConsumeGenerator.ID, (face, dTick, nodeEntity, state, installedPlugin) -> {
//            state.setSideAtlas(face, EtherAdapterNodeAtlas.FUNCTION_GRIND_EMPTY);
//            int workState = nodeEntity.getSyncedPluginData(installedPlugin, FunctionEquipmentConsumeGenerator.STATE);
//            if (workState == 1) {
            state.setSideAtlas(face, EtherAdapterNodeAtlas.FUNCTION_GRIND_WORKING);
//            }
            long maxEther = nodeEntity.getMaxEther();
            if (maxEther != 0)
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_FILL.get((int) Math.min((nodeEntity.getEther() * 10 / maxEther), 10)));
            else
                state.addOverlay(face, EtherAdapterNodeAtlas.OVERLAY_FUNCTION_BURNER_FILL.get(10));
        });
        register(FeatureContainerInteract.ID, (face, dTick, nodeEntity, state, installedPlugin) ->
                state.setSideAtlas(face, switch (face) {
                    case UP -> EtherAdapterNodeAtlas.FEATURE_CONTAINER_INT_TOP;
                    case DOWN -> EtherAdapterNodeAtlas.FEATURE_CONTAINER_INT_BOTTOM;
                    default -> EtherAdapterNodeAtlas.FEATURE_CONTAINER_INT_SIDE;
                }));
        register(FeatureDropperThrower.ID, (face, dTick, nodeEntity, state, installedPlugin) ->
                state.setSideAtlas(face, switch (face) {
                    case UP -> EtherAdapterNodeAtlas.FEATURE_DROPPER_TOP;
                    case DOWN -> EtherAdapterNodeAtlas.FEATURE_DROPPER_BOTTOM;
                    default -> EtherAdapterNodeAtlas.FEATURE_DROPPER_SIDE;
                }));
        register(FunctionMagnet.ID, (face, dTick, nodeEntity, state, installedPlugin) ->
                state.setSideAtlas(face, switch (face) {
                    case UP -> EtherAdapterNodeAtlas.FEATURE_MAGNET_TOP;
                    case DOWN -> EtherAdapterNodeAtlas.FEATURE_MAGNET_BOTTOM;
                    default -> EtherAdapterNodeAtlas.FEATURE_MAGNET_SIDE;
                }));
        register(FeatureEtherStreamEmitter.ID, (face, dTick, nodeEntity, state, installedPlugin) ->
                state.setSideAtlas(face, switch (face) {
                    case UP -> EtherAdapterNodeAtlas.FEATURE_STREAM_EMITTER_TOP;
                    case DOWN -> EtherAdapterNodeAtlas.FEATURE_STREAM_EMITTER_BOTTOM;
                    default -> EtherAdapterNodeAtlas.FEATURE_STREAM_EMITTER_SIDE;
                })
        );
    }

    public void register(Identifier id, PluginRender renderer) {
        pluginRenderer.put(id, renderer);
    }

    public void register(Identifier id, EtherAdapterNodeAtlas.AtlasUV renderer) {
        pluginRenderer.put(id, (d, _, _, s, ip) -> s.setSideAtlas(d, renderer));
    }

    public void render(Direction key, InstalledPlugin value, EtherAdaptNodeEntity entity, EtherAdapterNodeRenderState state) {
        LocalPlayer p = Minecraft.getInstance().player;
        int dTick = p == null ? 0 : p.tickCount;
        if (pluginRenderer.containsKey(value.pluginId())) {
            pluginRenderer.get(value.pluginId()).render(key, dTick, entity, state, value);
        }
    }
}
