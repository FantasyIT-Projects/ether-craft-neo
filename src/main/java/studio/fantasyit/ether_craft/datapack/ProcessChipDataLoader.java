package studio.fantasyit.ether_craft.datapack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import studio.fantasyit.ether_craft.block.factory.EtherProcessChipManager;

import java.util.HashMap;
import java.util.Map;

public class ProcessChipDataLoader extends SimpleJsonResourceReloadListener<EtherProcessChipManager.ProcessChipRecord> {

    public ProcessChipDataLoader() {
        super(EtherProcessChipManager.ProcessChipRecord.CODEC,FileToIdConverter.json("ether_process_chip"));
    }

    @Override
    protected void apply(Map<Identifier, EtherProcessChipManager.ProcessChipRecord> identifierProcessChipRecordMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        EtherProcessChipManager.update(identifierProcessChipRecordMap);
    }


}
