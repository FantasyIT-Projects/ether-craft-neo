package studio.fantasyit.ether_craft.stream.vholder;

import studio.fantasyit.ether_craft.stream.data.StreamExtraProperty;

public class StreamHolderPropertyCounter {
    private int countBlockCollide;
    private int countEntityCollide;
    private int countNotDisplayTime;

    public void addStream(StreamExtraProperty streamExtraProperty) {
        if (!streamExtraProperty.isDisplayTime) countNotDisplayTime++;
        if (!streamExtraProperty.noBlockHit) countBlockCollide++;
        if (!streamExtraProperty.noEntityHit) countEntityCollide++;
    }

    public void removeStream(StreamExtraProperty streamExtraProperty) {
        if (!streamExtraProperty.isDisplayTime) countNotDisplayTime--;
        if (!streamExtraProperty.noBlockHit) countBlockCollide--;
        if (!streamExtraProperty.noEntityHit) countEntityCollide--;
    }

    public boolean isNoBlockCollide() {
        return countBlockCollide == 0;
    }

    public boolean isNoEntityCollide() {
        return countEntityCollide == 0;
    }

    public boolean isDisplayTime() {
        return countNotDisplayTime == 0;
    }
}
