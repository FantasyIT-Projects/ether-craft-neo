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
            state.setSideAtlas(face, EtherAdapterNodeAtlas.FUNCTION_ITEM_CONSUME_GENERATOR);

            if (value != AbstractItemConsumeFunction.WorkingMaterial.IDLE)
                state.addOverlay(face, EtherAdapterNodeAtlas.FUNCTION_ITEM_CONSUME_WORKING);

            if (value == AbstractItemConsumeFunction.WorkingMaterial.COAL) {
                state.addOverlay(face, EtherAdapterNodeAtlas.FUNCTION_ITEM_CONSUME_MATERIAL_FIRE.get(dTick));
            } else if (value == AbstractItemConsumeFunction.WorkingMaterial.LAVA) {
                state.addOverlay(face, EtherAdapterNodeAtlas.FUNCTION_ITEM_CONSUME_MATERIAL_LAVA.get(dTick));
            } else if (value == AbstractItemConsumeFunction.WorkingMaterial.WOOD) {
                state.addOverlay(face, EtherAdapterNodeAtlas.FUNCTION_ITEM_CONSUME_MATERIAL_WOOD);
            } else if (value == AbstractItemConsumeFunction.WorkingMaterial.STONE) {
                state.addOverlay(face, EtherAdapterNodeAtlas.FUNCTION_ITEM_CONSUME_MATERIAL_STONE);
            } else if (value == AbstractItemConsumeFunction.WorkingMaterial.DEEPSLATE) {
                state.addOverlay(face, EtherAdapterNodeAtlas.FUNCTION_ITEM_CONSUME_MATERIAL_BASALT);
            } else if (value != AbstractItemConsumeFunction.WorkingMaterial.IDLE) {
                state.addOverlay(face, EtherAdapterNodeAtlas.FUNCTION_ITEM_CONSUME_MATERIAL_FIRE.get(dTick));
            }

            long maxEther = nodeEntity.getMaxEther();
            if (maxEther != 0)
                state.addOverlay(face, EtherAdapterNodeAtlas.ETHER_FILL.get((int) Math.min((nodeEntity.getEther() * 10 / maxEther), 10)));
            else
                state.addOverlay(face, EtherAdapterNodeAtlas.ETHER_FILL.get(9));
        };
        register(FunctionFurnaceGenerator.ID, generatorLayer);
        register(FunctionStoneGenerator.ID, generatorLayer);
        register(FunctionEquipmentConsumeGenerator.ID, generatorLayer);
        register(FunctionGrowthAccelerator.ID, (face, dTick, nodeEntity, state, installedPlugin) -> {
            for (Direction f : Direction.Plane.HORIZONTAL)
                state.setSideAtlas(f, EtherAdapterNodeAtlas.FUNCTION_ACCELERATE);
        });
        register(FunctionGrowthAccelerator.ID_ALL, (face, dTick, nodeEntity, state, installedPlugin) -> {
            for (Direction f : Direction.Plane.HORIZONTAL)
                state.setSideAtlas(f, EtherAdapterNodeAtlas.FUNCTION_ACCELERATE);
        });
        register(FunctionEtherConverter.ID, (face, dTick, nodeEntity, state, installedPlugin) -> {
            state.setSideAtlas(face, EtherAdapterNodeAtlas.FUNCTION_ETHER_CONVERT);
            int workState = nodeEntity.getSyncedPluginData(installedPlugin, FunctionEtherConverter.STATE);
            if (workState == 1) {
                state.addOverlay(face, EtherAdapterNodeAtlas.FUNCTION_ETHER_CONVERT_WORKING.get(dTick));
            }
        });
        register(FunctionEquipmentConsumeGenerator.ID, (face, dTick, nodeEntity, state, installedPlugin) -> {
            state.setSideAtlas(face, EtherAdapterNodeAtlas.FUNCTION_EQUIPMENT_CONSUME);
            int workState = nodeEntity.getSyncedPluginData(installedPlugin, FunctionEquipmentConsumeGenerator.STATE);
            if (workState == 1) {
                state.addOverlay(face, EtherAdapterNodeAtlas.FUNCTION_EQUIPMENT_CONSUME_WORKING.get(dTick));
            }
            long maxEther = nodeEntity.getMaxEther();
            if (maxEther != 0)
                state.addOverlay(face, EtherAdapterNodeAtlas.ETHER_FILL.get((int) Math.min((nodeEntity.getEther() * 10 / maxEther), 9)));
            else
                state.addOverlay(face, EtherAdapterNodeAtlas.ETHER_FILL.get(9));
        });
        register(FeatureContainerInteract.ID, (face, dTick, nodeEntity, state, installedPlugin) -> {
            boolean extractMode = nodeEntity.getSyncedPluginData(installedPlugin, FeatureContainerInteract.WORKING_MODE) == 1;
            if (extractMode)
                state.setSideAtlas(face, EtherAdapterNodeAtlas.FEATURE_INTERACT_EXTRACT);
            else
                state.setSideAtlas(face, EtherAdapterNodeAtlas.FEATURE_INTERACT_INSERT);
        });
        register(FeatureDropperThrower.ID, (face, dTick, nodeEntity, state, installedPlugin) -> {
            state.setSideAtlas(face, EtherAdapterNodeAtlas.FEATURE_DROPPER);
        });
        register(FunctionMagnet.ID, (face, dTick, nodeEntity, state, installedPlugin) -> {
                    state.setSideAtlas(Direction.DOWN, EtherAdapterNodeAtlas.FUNCTION_MAGNET_BOT.get(dTick));
                    state.setSideAtlas(Direction.UP, EtherAdapterNodeAtlas.FUNCTION_MAGNET_TOP.get(dTick));
                }
        );
        register(FunctionEnchanter.ID, (face, dTick, nodeEntity, state, installedPlugin) -> {
                    state.setSideAtlas(face, EtherAdapterNodeAtlas.FUNCTION_ENCHANT.get(dTick));
                }
        );
        register(FeatureEtherStreamEmitter.ID, (face, dTick, nodeEntity, state, installedPlugin) ->
                state.setSideAtlas(face, EtherAdapterNodeAtlas.FEATURE_EMITTER)
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
        int dTick = p == null ? 0 : (p.tickCount / 2);
        if (pluginRenderer.containsKey(value.pluginId())) {
            pluginRenderer.get(value.pluginId()).render(key, dTick, entity, state, value);
        }
    }
}
