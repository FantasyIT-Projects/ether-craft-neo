package studio.fantasyit.ether_craft.integration.iris;

import net.irisshaders.iris.api.v0.IrisApi;

public class IrisApiWrapper {
    public static boolean isIrisHasShaderLoaded() {
        return IrisApi.getInstance().isShaderPackInUse();
    }
}
