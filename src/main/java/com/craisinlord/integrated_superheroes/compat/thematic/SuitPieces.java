package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.registries.armors.armor.ArmorDisplayOrder;
import bond.thematic.api.registries.armors.armor.ArmorRegistry;
import bond.thematic.api.registries.armors.armor.CharacterAltItem;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import bond.thematic.api.registries.armors.armor.starter.StarterLegacyShell;
import bond.thematic.api.registries.data.stats.DisabledSuits;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SuitPieces {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, IntegratedSuperheroes.MODID);
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, IntegratedSuperheroes.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IntegratedSuperheroes.MODID);
    private static final String GROUP_PREFIX = "thematic_skin/";
    public static final TagKey<Item> HELMETLESS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "helmetless_suits"));
    public static final TagKey<Item> COSTUME = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "thematic_costume"));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SUIT = COMPONENTS.register("suit",
            () -> DataComponentType.<ResourceLocation>builder().persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC).build());

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MATERIAL = MATERIALS.register("suit_piece", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.HELMET, 1);
                map.put(ArmorItem.Type.CHESTPLATE, 3);
                map.put(ArmorItem.Type.LEGGINGS, 2);
                map.put(ArmorItem.Type.BOOTS, 1);
                map.put(ArmorItem.Type.BODY, 3);
            }),
            15, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "suit_piece"))), 0.0F, 0.0F));

    public static final DeferredItem<SuitPieceItem> HELMET = ITEMS.register("suit_helmet",
            () -> new SuitPieceItem(MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SuitPieceItem> LEGGINGS = ITEMS.register("suit_leggings",
            () -> new SuitPieceItem(MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SuitPieceItem> BOOTS = ITEMS.register("suit_boots",
            () -> new SuitPieceItem(MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().stacksTo(1)));

    private static final Map<Item, ResourceLocation> GROUPS = new ConcurrentHashMap<>();
    private static final Map<Item, ItemStack> STAND_INS = new ConcurrentHashMap<>();
    private static final Map<Integer, Check> CLIENT_CHECKS = new ConcurrentHashMap<>();
    private static final Map<Integer, Check> SERVER_CHECKS = new ConcurrentHashMap<>();

    private record Check(int tick, Item suit, boolean complete) {
    }

    public record Parts(boolean head, boolean body, boolean legs, boolean feet) {
    }

    private SuitPieces() {
    }

    static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        MATERIALS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(SuitPieces::onEquipmentChange);
        com.craisinlord.integrated_superheroes.cleanup.UnifiedCreativeTab.followers = SuitPieces::piecesFor;
        com.craisinlord.integrated_superheroes.cleanup.UnifiedCreativeTab.skip = stack -> stack.getItem() instanceof SuitPieceItem && suit(stack) == null;
        com.craisinlord.integrated_superheroes.cleanup.UnifiedCreativeTab.extras = SuitPieces::creativeSuits;
        com.craisinlord.integrated_superheroes.cleanup.UnifiedCreativeTab.claims = stack -> stack.getItem() instanceof CharacterItem;
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent event) -> GROUPS.clear());
    }

    public static Item suit(ItemStack stack) {
        if (!(stack.getItem() instanceof SuitPieceItem)) {
            return null;
        }
        ResourceLocation id = stack.get(SUIT.get());
        if (id == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item instanceof CharacterItem ? item : null;
    }

    public static ItemStack piece(EquipmentSlot slot, Item suit) {
        DeferredItem<SuitPieceItem> item = switch (slot) {
            case HEAD -> HELMET;
            case LEGS -> LEGGINGS;
            case FEET -> BOOTS;
            default -> null;
        };
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item.get());
        stack.set(SUIT.get(), BuiltInRegistries.ITEM.getKey(suit));
        return stack;
    }

    public static List<ItemStack> creativeSuits() {
        List<CharacterItem> suits = new java.util.ArrayList<>();
        for (CharacterItem armor : ArmorRegistry.getArmors()) {
            if (armor.inCreative() && !DisabledSuits.isDisabled(armor)) {
                suits.add(armor);
            }
        }
        suits.sort(ArmorDisplayOrder.COMPARATOR);
        List<CharacterItem> alts = new java.util.ArrayList<>();
        for (CharacterAltItem armor : ArmorRegistry.getAlts()) {
            if (!(armor instanceof StarterLegacyShell) && !DisabledSuits.isDisabled(armor)) {
                alts.add(armor);
            }
        }
        alts.sort(ArmorDisplayOrder.COMPARATOR);
        suits.addAll(alts);
        if (suits.isEmpty()) {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof CharacterItem armor && !(armor instanceof StarterLegacyShell) && !DisabledSuits.isDisabled(armor)) {
                    suits.add(armor);
                }
            }
        }
        List<ItemStack> stacks = new java.util.ArrayList<>(suits.size());
        for (CharacterItem suit : suits) {
            stacks.add(new ItemStack(suit));
        }
        return stacks;
    }

    public static List<ItemStack> piecesFor(ItemStack stack) {
        if (!(stack.getItem() instanceof CharacterItem suit)) {
            return List.of();
        }
        List<ItemStack> pieces = new java.util.ArrayList<>(3);
        if (!helmetless(suit)) {
            pieces.add(piece(EquipmentSlot.HEAD, suit));
        }
        pieces.add(piece(EquipmentSlot.LEGS, suit));
        pieces.add(piece(EquipmentSlot.FEET, suit));
        return pieces;
    }

    @SuppressWarnings("deprecation")
    private static ResourceLocation group(Item item) {
        ResourceLocation base = com.craisinlord.integrated_superheroes.cleanup.CosmeticMerges.baseSuit(BuiltInRegistries.ITEM.getKey(item));
        if (base != null) return IntegratedSuperheroes.id(GROUP_PREFIX + base.getPath());
        return GROUPS.computeIfAbsent(item, key -> key.builtInRegistryHolder().tags()
                .map(TagKey::location)
                .filter(location -> location.getNamespace().equals(IntegratedSuperheroes.MODID) && location.getPath().startsWith(GROUP_PREFIX))
                .findFirst()
                .orElse(BuiltInRegistries.ITEM.getKey(key)));
    }

    private static boolean belongs(ItemStack stack, Item suit) {
        Item piece = suit(stack);
        return piece != null && (piece == suit || group(piece).equals(group(suit)));
    }

    @SuppressWarnings("deprecation")
    public static boolean costume(CharacterItem suit) {
        return PowerOwnership.costume(BuiltInRegistries.ITEM.getKey(suit).toString())
                || suit.builtInRegistryHolder().is(COSTUME) || suit.getAbilities().isEmpty();
    }

    @SuppressWarnings("deprecation")
    public static boolean helmetless(Item suit) {
        return suit.builtInRegistryHolder().is(HELMETLESS);
    }

    private static boolean wears(LivingEntity entity, EquipmentSlot slot, DeferredItem<SuitPieceItem> type, Item suit) {
        ItemStack stack = entity.getItemBySlot(slot);
        return stack.is(type.get()) && belongs(stack, suit);
    }

    public static boolean complete(Player player, Item suit) {
        Map<Integer, Check> checks = player.level().isClientSide ? CLIENT_CHECKS : SERVER_CHECKS;
        Check check = checks.get(player.getId());
        if (check != null && check.tick() == player.tickCount && check.suit() == suit) {
            return check.complete();
        }
        boolean complete = wears(player, EquipmentSlot.LEGS, LEGGINGS, suit) && wears(player, EquipmentSlot.FEET, BOOTS, suit)
                && (helmetless(suit) || wears(player, EquipmentSlot.HEAD, HELMET, suit));
        checks.put(player.getId(), new Check(player.tickCount, suit, complete));
        return complete;
    }

    public static ItemStack standIn(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return null;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof CharacterItem) {
            return null;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            Item suit = suit(player.getItemBySlot(slot));
            if (suit != null) {
                return STAND_INS.computeIfAbsent(suit, ItemStack::new);
            }
        }
        return null;
    }

    public static boolean partial(Player player, Item rendered) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() == rendered) {
            return !complete(player, rendered);
        }
        return STAND_INS.containsKey(rendered) && (belongs(player.getItemBySlot(EquipmentSlot.HEAD), rendered)
                || belongs(player.getItemBySlot(EquipmentSlot.LEGS), rendered) || belongs(player.getItemBySlot(EquipmentSlot.FEET), rendered));
    }

    public static Parts parts(Player player, ItemStack rendered) {
        if (rendered == null || rendered.isEmpty() || !(rendered.getItem() instanceof CharacterItem)) {
            return null;
        }
        Item suit = rendered.getItem();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean body = chest.getItem() == suit;
        if (!body && STAND_INS.get(suit) != rendered) {
            return null;
        }
        boolean head = wears(player, EquipmentSlot.HEAD, HELMET, suit) || body && helmetless(suit);
        return new Parts(head, body, wears(player, EquipmentSlot.LEGS, LEGGINGS, suit), wears(player, EquipmentSlot.FEET, BOOTS, suit));
    }

    private static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSlot() != EquipmentSlot.CHEST) {
            return;
        }
        ItemStack chest = event.getTo();
        if (chest.getItem() instanceof CharacterItem suit && event.getFrom().getItem() != suit && !costume(suit) && !complete(player, suit)) {
            player.displayClientMessage(Component.translatable("message.integrated_superheroes.suit_incomplete"), true);
        }
    }
}
