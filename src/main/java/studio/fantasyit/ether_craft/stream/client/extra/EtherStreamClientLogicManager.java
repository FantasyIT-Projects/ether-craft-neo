package studio.fantasyit.ether_craft.stream.client.extra;

import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;

import java.util.ArrayList;
import java.util.List;

public class EtherStreamClientLogicManager {
    public static List<IEtherStreamExtraClientLogic> extraLogic = new ArrayList<>();

    public static void collect() {
        extraLogic.add(new EtherStreamLabelLogic());
        extraLogic.add(new EtherStreamCarriedEntityLogic());
    }

    public static void reApplyAttach(ClientStreamEntry entry) {
        for (IEtherStreamExtraClientLogic logic : extraLogic) {
            if (logic.shouldAttach(entry)) {
                entry.attachedLogic.add(logic);
                logic.onAttach(entry);
            } else if (entry.attachedLogic.contains(logic)) {
                logic.onDetach(entry);
                entry.attachedLogic.remove(logic);
            }
        }
    }
}