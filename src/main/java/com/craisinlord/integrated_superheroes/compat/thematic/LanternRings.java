package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.ability.corp.CorpsType;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.cleanup.UnifiedCreativeTab;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class LanternRings {
    private static final String MADE = "integrated_superheroes_ring_made";
    private static final String LOOK = "integrated_superheroes_lantern_look";
    private static final String SAVED = "integrated_superheroes_lantern_suits";
    private static final TagKey<Item> LANTERN_SUITS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "lantern_suits"));
    private static final Map<String, String> BASE_LOOKS = Map.of(
            "willpower", "hal_jordan",
            "fear", "sinestro",
            "rage", "atrocitus",
            "hope", "saint_walker",
            "avarice", "larfleeze",
            "compassion", "indigo1",
            "love", "star_sapphire",
            "death", "black_hand",
            "life", "white_lantern");
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int OFFHAND_SLOT = 40;
    private static boolean curios;
    private static final Map<Item, String> PREFIXES = new java.util.concurrent.ConcurrentHashMap<>();

    private LanternRings() {
    }

    static void register() {
        curios = ModList.get().isLoaded("curios");
        if (!curios) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(LanternRings::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(LanternRings::onJoin);
        NeoForge.EVENT_BUS.addListener(LanternRings::onDrops);
        NeoForge.EVENT_BUS.addListener(LanternRings::onUse);
        NeoForge.EVENT_BUS.addListener(LanternRings::onTooltip);
        java.util.function.Supplier<List<ItemStack>> prior = UnifiedCreativeTab.extras;
        UnifiedCreativeTab.extras = () -> {
            List<ItemStack> out = new ArrayList<>(prior.get());
            out.addAll(ringStacks());
            return out;
        };
    }

    private static List<ItemStack> ringStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (CorpsType corps : CorpsType.values()) {
            try {
                if (BASE_LOOKS.containsKey(corps.getPrefix()) && corps.getRing() != null) {
                    out.add(new ItemStack(corps.getRing()));
                }
            } catch (RuntimeException ignored) {
            }
        }
        return out;
    }

    public static boolean wearsRing(Player player) {
        return player != null && !wornRing(player).isEmpty();
    }

    private static ItemStack wornRing(LivingEntity entity) {
        if (!curios) {
            return ItemStack.EMPTY;
        }
        try {
            return CuriosRings.find(entity, LanternRings::isRing);
        } catch (Throwable error) {
            curios = false;
            IntegratedSuperheroes.LOGGER.warn("Lantern rings turned off: couldn't read the ring slot", error);
            return ItemStack.EMPTY;
        }
    }

    private static String prefix(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String prefix = PREFIXES.computeIfAbsent(stack.getItem(), LanternRings::lookupPrefix);
        return prefix.isEmpty() ? null : prefix;
    }

    private static String lookupPrefix(Item item) {
        for (CorpsType corps : CorpsType.values()) {
            try {
                if (corps.getRing() == item) {
                    return corps.getPrefix();
                }
            } catch (RuntimeException ignored) {
            }
        }
        return "";
    }

    private static boolean isRing(ItemStack stack) {
        String prefix = prefix(stack);
        return prefix != null && BASE_LOOKS.containsKey(prefix);
    }

    private static TagKey<Item> looksTag(String prefix) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "lantern_looks/" + prefix));
    }

    private static Item baseLook(String prefix) {
        String id = BASE_LOOKS.get(prefix);
        return id == null ? null : BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("thematic", id))
                .filter(item -> item instanceof CharacterItem).orElse(null);
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static Item lookItem(String id, String prefix) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(key).orElse(null);
        return item instanceof CharacterItem && new ItemStack(item).is(looksTag(prefix)) ? item : null;
    }

    private static Item look(ItemStack ring, String prefix) {
        CompoundTag tag = data(ring);
        if (tag.contains(LOOK, Tag.TAG_STRING)) {
            Item chosen = lookItem(tag.getString(LOOK), prefix);
            if (chosen != null) {
                return chosen;
            }
        }
        return baseLook(prefix);
    }

    private static List<Item> looks(String prefix) {
        List<Item> out = new ArrayList<>();
        Item base = baseLook(prefix);
        if (base != null) {
            out.add(base);
        }
        List<Item> tagged = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(looksTag(prefix)).forEach(holder -> {
            Item item = holder.value();
            if (item instanceof CharacterItem && !out.contains(item) && !tagged.contains(item)) {
                tagged.add(item);
            }
        });
        tagged.sort(java.util.Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).getPath()));
        out.addAll(tagged);
        return out;
    }

    private static void setLook(ItemStack ring, Item item) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        CustomData.update(DataComponents.CUSTOM_DATA, ring, tag -> tag.putString(LOOK, id));
    }

    static boolean made(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(MADE);
    }

    private static ItemStack mark(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(MADE, true));
        return stack;
    }

    private static ItemStack unmark(ItemStack stack) {
        ItemStack copy = stack.copy();
        CustomData.update(DataComponents.CUSTOM_DATA, copy, tag -> tag.remove(MADE));
        return copy;
    }

    private static boolean lanternGear(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof CharacterItem) {
            return stack.is(LANTERN_SUITS);
        }
        Item suit = SuitPieces.suit(stack);
        return suit != null && new ItemStack(suit).is(LANTERN_SUITS);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator() || player.tickCount % 5 != 0) {
            return;
        }
        ItemStack ring = wornRing(player);
        String prefix = prefix(ring);
        Item want = prefix == null ? null : look(ring, prefix);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (made(chest) && chest.getItem() != want) {
            strip(player);
        }
        if (want != null) {
            dress(player, want);
        }
        evict(player);
        purge(player);
    }

    private static boolean fits(ItemStack current, EquipmentSlot slot, Item want) {
        if (!made(current)) {
            return false;
        }
        return slot == EquipmentSlot.CHEST ? current.is(want) : SuitPieces.suit(current) == want;
    }

    private static void dress(ServerPlayer player, Item want) {
        boolean changed = false;
        boolean helmetless = SuitPieces.helmetless(want);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.CHEST}) {
            ItemStack current = player.getItemBySlot(slot);
            if (slot == EquipmentSlot.HEAD && helmetless) {
                if (made(current)) {
                    player.setItemSlot(slot, ItemStack.EMPTY);
                }
                continue;
            }
            if (fits(current, slot, want)) {
                continue;
            }
            if (!current.isEmpty() && !made(current)) {
                give(player, current.copy());
            }
            player.setItemSlot(slot, mark(slot == EquipmentSlot.CHEST ? savedSuit(player, want) : SuitPieces.piece(slot, want)));
            changed = true;
        }
        if (changed) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.3F);
        }
    }

    private static ItemStack savedSuit(ServerPlayer player, Item want) {
        CompoundTag saved = player.getPersistentData().getCompound(SAVED);
        String key = BuiltInRegistries.ITEM.getKey(want).toString();
        if (saved.contains(key, Tag.TAG_COMPOUND)) {
            ItemStack stack = ItemStack.parseOptional(player.registryAccess(), saved.getCompound(key));
            if (stack.is(want)) {
                return stack;
            }
        }
        return new ItemStack(want);
    }

    private static void save(ServerPlayer player, ItemStack chest) {
        if (chest.isEmpty()) {
            return;
        }
        CompoundTag saved = player.getPersistentData().getCompound(SAVED);
        saved.put(BuiltInRegistries.ITEM.getKey(chest.getItem()).toString(), unmark(chest).save(player.registryAccess()));
        player.getPersistentData().put(SAVED, saved);
    }

    private static void strip(ServerPlayer player) {
        boolean any = false;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack current = player.getItemBySlot(slot);
            if (made(current)) {
                if (slot == EquipmentSlot.CHEST) {
                    save(player, current);
                }
                player.setItemSlot(slot, ItemStack.EMPTY);
                any = true;
            }
        }
        if (any) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.3F);
        }
    }

    private static void evict(ServerPlayer player) {
        boolean any = false;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack current = player.getItemBySlot(slot);
            if (!made(current) && lanternGear(current)) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                give(player, current.copy());
                any = true;
            }
        }
        if (any) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.lantern.needs_ring"), true);
        }
    }

    private static void purge(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (made(inventory.items.get(slot))) {
                inventory.items.set(slot, ItemStack.EMPTY);
            }
        }
        if (made(inventory.getItem(OFFHAND_SLOT))) {
            inventory.setItem(OFFHAND_SLOT, ItemStack.EMPTY);
        }
        if (player.containerMenu != player.inventoryMenu && made(player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity item && made(item.getItem())) {
            event.setCanceled(true);
        }
    }

    private static void onDrops(LivingDropsEvent event) {
        event.getDrops().removeIf(item -> made(item.getItem()));
    }

    private static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack ring = event.getItemStack();
        String prefix = prefix(ring);
        if (prefix == null || !BASE_LOOKS.containsKey(prefix)) {
            return;
        }
        Player player = event.getEntity();
        if (player.isShiftKeyDown()) {
            if (!player.level().isClientSide()) {
                List<Item> options = looks(prefix);
                if (options.size() < 2) {
                    player.displayClientMessage(Component.translatable("message.integrated_superheroes.lantern.one_look"), true);
                } else {
                    Item next = options.get((options.indexOf(look(ring, prefix)) + 1) % options.size());
                    setLook(ring, next);
                    player.displayClientMessage(Component.translatable("message.integrated_superheroes.lantern.look",
                            new ItemStack(next).getHoverName()), true);
                }
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String prefix = prefix(stack);
        if (prefix == null || !BASE_LOOKS.containsKey(prefix)) {
            return;
        }
        Item look = look(stack, prefix);
        if (look != null) {
            event.getToolTip().add(Component.translatable("tooltip.integrated_superheroes.lantern.look", new ItemStack(look).getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        int count = looks(prefix).size();
        if (count > 1) {
            event.getToolTip().add(Component.translatable("tooltip.integrated_superheroes.lantern.looks", count)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
