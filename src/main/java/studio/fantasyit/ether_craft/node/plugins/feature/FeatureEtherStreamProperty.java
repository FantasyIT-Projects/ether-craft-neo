package studio.fantasyit.ether_craft.node.plugins.feature;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.IEtherStreamCapabilityProviderPlugin;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.data.StreamExtraProperty;

public class FeatureEtherStreamProperty extends AbstractNodePlugin implements IEtherStreamCapabilityProviderPlugin {
    public static final Identifier ID = EtherCraft.id("ether_stream_property");
    public static final Identifier SYNC_DISPLAY_TIME = EtherCraft.id("stream_property/display_time");
    public static final Identifier SYNC_LIMIT_ENABLED = EtherCraft.id("stream_property/limit_enabled");
    public static final Identifier SYNC_MAX_TRAVEL = EtherCraft.id("stream_property/max_travel");
    public static final Identifier SYNC_NO_ENTITY_HIT = EtherCraft.id("stream_property/no_entity_hit");
    public static final Identifier SYNC_NO_BLOCK_HIT = EtherCraft.id("stream_property/no_block_hit");

    public static final float MAX_TRAVEL_LENGTH = 100000f;
    private static final int SCALE = 100;

    public boolean isDisplayTime = false;
    public boolean limitEnabled = false;
    public float maxTravelLength = 0f;
    public boolean noEntityHit = false;
    public boolean noBlockHit = false;

    public FeatureEtherStreamProperty(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void provideCapabilities(IEtherStreamLike entity, StreamExtraProperty extraProperty) {
        extraProperty.isDisplayTime = isDisplayTime;
        if (limitEnabled && maxTravelLength > 0f) {
            extraProperty.maxTravelLength = maxTravelLength;
        }
        extraProperty.noEntityHit = noEntityHit;
        extraProperty.noBlockHit = noBlockHit;
    }

    public static int toIntData(float value) {
        return (int) (value * SCALE);
    }

    public static float fromIntData(int data) {
        return data / (float) SCALE;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("displayTime", isDisplayTime);
        output.putBoolean("limitEnabled", limitEnabled);
        output.store("maxTravel", Codec.FLOAT, maxTravelLength);
        output.putBoolean("noEntityHit", noEntityHit);
        output.putBoolean("noBlockHit", noBlockHit);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        isDisplayTime = input.getBooleanOr("displayTime", false);
        limitEnabled = input.getBooleanOr("limitEnabled", false);
        maxTravelLength = input.read("maxTravel", Codec.FLOAT).orElse(0f);
        noEntityHit = input.getBooleanOr("noEntityHit", false);
        noBlockHit = input.getBooleanOr("noBlockHit", false);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        super.syncScreenData(message);
        if (message.id().equals(SYNC_DISPLAY_TIME)) {
            isDisplayTime = message.data() == 1;
            nodeEntity.setChanged();
        }
        if (message.id().equals(SYNC_LIMIT_ENABLED)) {
            limitEnabled = message.data() == 1;
            nodeEntity.setChanged();
        }
        if (message.id().equals(SYNC_MAX_TRAVEL)) {
            maxTravelLength = Math.clamp(fromIntData(message.data()), 0f, MAX_TRAVEL_LENGTH);
            nodeEntity.setChanged();
        }
        if (message.id().equals(SYNC_NO_ENTITY_HIT)) {
            noEntityHit = message.data() == 1;
            nodeEntity.setChanged();
        }
        if (message.id().equals(SYNC_NO_BLOCK_HIT)) {
            noBlockHit = message.data() == 1;
            nodeEntity.setChanged();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> isDisplayTime ? 1 : 0, t -> isDisplayTime = t == 1));
        menu.addDataSlot(new BaseDataSlot(() -> limitEnabled ? 1 : 0, t -> limitEnabled = t == 1));
        menu.addDataSlot(new BaseDataSlot(() -> toIntData(maxTravelLength), t -> maxTravelLength = Math.clamp(fromIntData(t), 0f, MAX_TRAVEL_LENGTH)));
        menu.addDataSlot(new BaseDataSlot(() -> noEntityHit ? 1 : 0, t -> noEntityHit = t == 1));
        menu.addDataSlot(new BaseDataSlot(() -> noBlockHit ? 1 : 0, t -> noBlockHit = t == 1));
    }
}
