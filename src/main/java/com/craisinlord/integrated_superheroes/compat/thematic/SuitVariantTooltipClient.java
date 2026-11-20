package com.craisinlord.integrated_superheroes.compat.thematic;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID, value = Dist.CLIENT)
public final class SuitVariantTooltipClient {
    private static final String PREFIX = "thematic_skin/";
    private static final TagKey<Item> COSTUME = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "thematic_costume"));
    private static final Set<String> RARITIES = Set.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY");
    private static Map<Item, Item> rootOf;
    private static Map<Item, Integer> looks;

    private SuitVariantTooltipClient() {
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        rootOf = null;
        looks = null;
    }

    private static void build() {
        Map<Item, Item> roots = new HashMap<>();
        Map<Item, Integer> counts = new HashMap<>();
        BuiltInRegistries.ITEM.getTags().forEach(pair -> {
            TagKey<Item> tag = pair.getFirst();
            ResourceLocation id = tag.location();
            if (!id.getNamespace().equals(IntegratedSuperheroes.MODID) || !id.getPath().startsWith(PREFIX)) {
                return;
            }
            int size = pair.getSecond().size();
            if (size < 2) {
                return;
            }
            Item root = null;
            for (Holder<Item> member : pair.getSecond()) {
                if (!"thematic".equals(BuiltInRegistries.ITEM.getKey(member.value()).getNamespace())) {
                    root = member.value();
                    break;
                }
            }
            if (root == null) {
                root = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thematic", id.getPath().substring(PREFIX.length())));
            }
            if (root == Items.AIR) {
                return;
            }
            counts.put(root, size - 1);
            for (Holder<Item> member : pair.getSecond()) {
                if (member.value() != root) {
                    roots.putIfAbsent(member.value(), root);
                }
            }
        });
        rootOf = roots;
        looks = counts;
    }

    public static List<Component> decorate(ItemStack stack, List<Component> lines) {
        if (stack.isEmpty() || lines.isEmpty()) {
            return lines;
        }
        Item item = stack.getItem();
        boolean costume = stack.is(COSTUME);
        if (rootOf == null) {
            build();
        }
        Item root = rootOf.get(item);
        Integer count = root == null ? looks.get(item) : null;
        if (!costume && root == null && count == null) {
            return lines;
        }
        List<Component> result = new ArrayList<>(lines);
        if (costume) {
            stripSuitInfo(stack, result);
        }
        if (root != null) {
            result.add(1, Component.translatable("tooltip.integrated_superheroes.suit_variant",
                    root.getDescription()).withStyle(ChatFormatting.GRAY));
        } else if (count != null) {
            result.add(1, Component.translatable("tooltip.integrated_superheroes.suit_base", count)
                    .withStyle(ChatFormatting.GRAY));
        }
        return result;
    }

    private static void stripSuitInfo(ItemStack stack, List<Component> lines) {
        int tier = -1;
        for (int i = 1; i < lines.size(); i++) {
            if (plain(lines.get(i)).startsWith("Tier: ")) {
                tier = i;
                break;
            }
        }
        if (tier < 0) {
            return;
        }
        int cut = tier;
        if (cut > 1 && plain(lines.get(cut - 1)).isBlank()) {
            cut--;
        }
        if (cut > 1 && RARITIES.contains(plain(lines.get(cut - 1)))) {
            cut--;
        }
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        List<Component> kept = new ArrayList<>();
        for (Component line : lines.subList(cut, lines.size())) {
            if (key.equals(plain(line)) || line.getContents() instanceof TranslatableContents translatable
                    && "item.components".equals(translatable.getKey())) {
                kept.add(line);
            }
        }
        lines.subList(cut, lines.size()).clear();
        lines.addAll(kept);
    }

    private static String plain(Component line) {
        String text = ChatFormatting.stripFormatting(line.getString());
        return text == null ? "" : text.trim();
    }
}
