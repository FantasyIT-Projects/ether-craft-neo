package studio.fantasyit.ether_craft.datagen;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.List;

public class ItemFormatParser {

    static ItemStackTemplate parseOutputTarget(String raw) {
        if (raw == null || raw.isEmpty()) {
            return new ItemStackTemplate(Items.AIR, 1);
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                JsonElement json = JsonParser.parseString(trimmed);
                if (json.isJsonArray()) {
                    var result = ItemStackTemplate.CODEC.listOf().parse(JsonOps.INSTANCE, json);
                    if (result.isSuccess() && !result.getOrThrow().isEmpty()) {
                        return result.getOrThrow().get(0);
                    }
                    return new ItemStackTemplate(Items.AIR, 1);
                }
                // Try ItemStackTemplate first
                var obj = json.getAsJsonObject();
                if (obj.has("id")) {
                    return ItemStackTemplate.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
                }
                // Handle DataComponentIngredient format: {neoforge:ingredient_type, items, components}
                if (obj.has("neoforge:ingredient_type") && obj.has("items")) {
                    String itemId = obj.get("items").getAsString();
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
                    if (obj.has("components")) {
                        DataComponentPatch patch = DataComponentPatch.CODEC
                                .parse(JsonOps.INSTANCE, obj.get("components")).getOrThrow();
                        return new ItemStackTemplate(item, 1, patch);
                    }
                    return new ItemStackTemplate(item, 1);
                }
                return ItemStackTemplate.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            } catch (Exception ignored) {
            }
        }
        Pair<String, Integer> parsed = parseWithCount(trimmed);
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(parsed.id()));
        return new ItemStackTemplate(item, parsed.count());
    }

    static List<ItemStackTemplate> parseOutputItems(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of(new ItemStackTemplate(Items.AIR, 1));
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                JsonElement json = JsonParser.parseString(trimmed);
                if (json.isJsonArray()) {
                    var result = ItemStackTemplate.CODEC.listOf().parse(JsonOps.INSTANCE, json);
                    if (result.isSuccess()) {
                        return result.getOrThrow();
                    }
                }
                var obj = json.getAsJsonObject();
                if (obj.has("id")) {
                    return List.of(ItemStackTemplate.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
                }
                if (obj.has("neoforge:ingredient_type") && obj.has("items")) {
                    String itemId = obj.get("items").getAsString();
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
                    if (obj.has("components")) {
                        DataComponentPatch patch = DataComponentPatch.CODEC
                                .parse(JsonOps.INSTANCE, obj.get("components")).getOrThrow();
                        return List.of(new ItemStackTemplate(item, 1, patch));
                    }
                    return List.of(new ItemStackTemplate(item, 1));
                }
                return List.of(ItemStackTemplate.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
            } catch (Exception ignored) {
            }
        }
        Pair<String, Integer> parsed = parseWithCount(trimmed);
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(parsed.id()));
        return List.of(new ItemStackTemplate(item, parsed.count()));
    }

    static SizedIngredient parseInputSizedIngredient(HolderLookup.Provider registries, String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                JsonElement json = JsonParser.parseString(trimmed);
                // Try SizedIngredient.NESTED_CODEC format first
                var ops2 = registries.createSerializationContext(JsonOps.INSTANCE);
                var result = SizedIngredient.NESTED_CODEC.parse(ops2, json);
                if (result.isSuccess()) return result.getOrThrow();
                Ingredient ing = Ingredient.CODEC.parse(ops2, json).getOrThrow();
                return new SizedIngredient(ing, 1);
            } catch (Exception ignored) {
            }
        }
        Pair<String, Integer> parsed = parseWithCount(trimmed);
        if (parsed.id().startsWith("#")) {
            String tagName = parsed.id().substring(1);
            return new SizedIngredient(lookupTag(registries, tagName), parsed.count());
        }
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(parsed.id()));
        return SizedIngredient.of(item, parsed.count());
    }

    static SizedIngredient parseInputForDetection(HolderLookup.Provider registries, String raw) {
        if (raw == null || raw.isEmpty()) {
            return SizedIngredient.of(Items.AIR, 1);
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                JsonElement json = JsonParser.parseString(trimmed);
                var ops = registries.createSerializationContext(JsonOps.INSTANCE);
                var result = SizedIngredient.NESTED_CODEC.parse(ops, json);
                if (result.isSuccess()) return result.getOrThrow();
                Ingredient ing = Ingredient.CODEC.parse(ops, json).getOrThrow();
                return new SizedIngredient(ing, 1);
            } catch (Exception ignored) {
            }
        }
        Pair<String, Integer> parsed = parseWithCount(trimmed);
        if (parsed.id().startsWith("#")) {
            String tagName = parsed.id().substring(1);
            return new SizedIngredient(lookupTag(registries, tagName), parsed.count());
        }
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(parsed.id()));
        return SizedIngredient.of(item, parsed.count());
    }

    static ItemStackTemplate makeChipTemplate(String chipId, int count) {
        Identifier id = Identifier.parse(chipId);
        DataComponentPatch patch = DataComponentPatch.builder()
                .set(DataComponentRegistry.CHIP_ID.get(), id)
                .set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath()))
                .build();
        return new ItemStackTemplate(ItemRegistry.PROCESS_CHIP_ITEM.get(), count, patch);
    }

    static ItemStackTemplate makeChipTemplate(String chipId) {
        return makeChipTemplate(chipId, 1);
    }

    static ItemStackTemplate makeSeparatorTemplate() {
        return makeChipTemplate("ether_craft:separator_chip");
    }

    private static Pair<String, Integer> parseWithCount(String raw) {
        int colonIndex = raw.lastIndexOf("::");
        if (colonIndex > 0 && colonIndex + 2 < raw.length()) {
            String after = raw.substring(colonIndex + 2);
            if (after.matches("\\d+")) {
                return new Pair<>(raw.substring(0, colonIndex), Integer.parseInt(after));
            }
        }
        return new Pair<>(raw, 1);
    }

    private static Ingredient lookupTag(HolderLookup.Provider registries, String tagName) {
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, Identifier.parse(tagName));
        return registries.lookupOrThrow(Registries.ITEM)
                .get(tagKey)
                .map(Ingredient::of)
                .orElseThrow(() -> new IllegalArgumentException("Unknown tag: #" + tagName));
    }

    private record Pair<A, B>(A id, B count) {}
}
