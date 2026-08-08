package studio.fantasyit.ether_craft.stream.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class StreamExtraProperty {
    public boolean isDisplayTime = false;
    public float maxTravelLength = Float.MAX_VALUE;
    public boolean noEntityHit = false;
    public boolean noBlockHit = false;
    public boolean noEtherCost = false;

    public StreamExtraProperty() {
    }

    public StreamExtraProperty(boolean isDisplayTime, float maxTravelLength, boolean noEntityHit, boolean noBlockHit, boolean noEtherCost) {
        this.isDisplayTime = isDisplayTime;
        this.maxTravelLength = maxTravelLength;
        this.noEntityHit = noEntityHit;
        this.noBlockHit = noBlockHit;
        this.noEtherCost = noEtherCost;
    }
    public void from(StreamExtraProperty extraProperty) {
        this.isDisplayTime = extraProperty.isDisplayTime;
        this.maxTravelLength = extraProperty.maxTravelLength;
        this.noEntityHit = extraProperty.noEntityHit;
        this.noBlockHit = extraProperty.noBlockHit;
        this.noEtherCost = extraProperty.noEtherCost;
    }

    public static final Codec<StreamExtraProperty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("isDisplayTime").orElse(false).forGetter(p -> p.isDisplayTime),
            Codec.FLOAT.fieldOf("maxTravelLength").orElse(Float.MAX_VALUE).forGetter(p -> p.maxTravelLength),
            Codec.BOOL.fieldOf("noEntityHit").orElse(false).forGetter(p -> p.noEntityHit),
            Codec.BOOL.fieldOf("noBlockHit").orElse(false).forGetter(p -> p.noBlockHit),
            Codec.BOOL.fieldOf("noEtherCost").orElse(false).forGetter(p -> p.noEtherCost)
    ).apply(instance, StreamExtraProperty::new));
}
