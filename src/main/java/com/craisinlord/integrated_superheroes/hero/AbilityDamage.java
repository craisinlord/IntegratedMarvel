package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityDamage {
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, IntegratedSuperheroes.MODID);
    public static final DeferredHolder<Attribute, Attribute> ATTRIBUTE = ATTRIBUTES.register("ability_damage",
            () -> new RangedAttribute("attribute.name.integrated_superheroes.ability_damage", 1.0D, 0.0D, 1024.0D).setSyncable(true));
    public static final DataMapType<Item, Double> ITEMS = DataMapType.builder(IntegratedSuperheroes.id("ability_damage"), Registries.ITEM,
            Codec.doubleRange(0.0D, 1024.0D)).synced(Codec.doubleRange(0.0D, 1024.0D), false).build();
    private static final TagKey<Item> HELMETLESS = TagKey.create(Registries.ITEM, IntegratedSuperheroes.id("helmetless_suits"));
    private static final Map<List<? extends String>, List<double[]>> CURVES = new ConcurrentHashMap<>();
    private static final double HELMETLESS_HEAD_SHARE = 0.15D / 0.55D;

    private AbilityDamage() {
    }

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener((EntityAttributeModificationEvent event) -> event.add(EntityType.PLAYER, ATTRIBUTE));
        modEventBus.addListener((RegisterDataMapTypesEvent event) -> event.register(ITEMS));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, AbilityDamage::onItemAttributes);
    }

    public static float scale(Entity caster) {
        if (!(caster instanceof Player player)) {
            return 1.0F;
        }
        AttributeInstance instance = player.getAttribute(ATTRIBUTE);
        return instance == null ? 1.0F : (float) instance.getValue();
    }

    public static float scale(Entity caster, Entity target) {
        return target instanceof Player ? 1.0F : scale(caster);
    }

    private static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        EquipmentSlot slot = null;
        double armor = 0.0D;
        for (ItemAttributeModifiers.Entry entry : event.getModifiers()) {
            if (!entry.attribute().is(Attributes.ARMOR.unwrapKey().orElseThrow()) || entry.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) {
                continue;
            }
            EquipmentSlot worn = slot(entry.slot());
            if (worn != null && (slot == null || slot == worn)) {
                slot = worn;
                armor += entry.modifier().amount();
            }
        }
        if (slot == null) {
            return;
        }
        Holder<Item> item = stack.getItemHolder();
        Double listed = item.getData(ITEMS);
        double bonus = listed != null ? listed : fromArmor(slot, armor, item.is(HELMETLESS));
        bonus = Math.round(bonus * 100.0D) / 100.0D;
        if (bonus > 0.0D) {
            event.addModifier(ATTRIBUTE, new AttributeModifier(IntegratedSuperheroes.id("ability_damage_" + slot.getName()), bonus,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.bySlot(slot));
        }
    }

    private static EquipmentSlot slot(EquipmentSlotGroup group) {
        return switch (group) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static double fromArmor(EquipmentSlot slot, double armor, boolean helmetless) {
        if (!MarvelBalanceConfig.SPEC.isLoaded()) {
            return 0.0D;
        }
        if (slot == EquipmentSlot.CHEST && helmetless) {
            double head = armor * HELMETLESS_HEAD_SHARE;
            return curve(EquipmentSlot.CHEST, armor - head) + curve(EquipmentSlot.HEAD, head);
        }
        return curve(slot, armor);
    }

    private static double curve(EquipmentSlot slot, double armor) {
        List<? extends String> raw = switch (slot) {
            case HEAD -> MarvelBalanceConfig.ABILITY_DAMAGE_HELMET.get();
            case CHEST -> MarvelBalanceConfig.ABILITY_DAMAGE_CHESTPLATE.get();
            case LEGS -> MarvelBalanceConfig.ABILITY_DAMAGE_LEGGINGS.get();
            default -> MarvelBalanceConfig.ABILITY_DAMAGE_BOOTS.get();
        };
        List<double[]> points = CURVES.computeIfAbsent(raw, AbilityDamage::parse);
        if (points.isEmpty() || armor < points.get(0)[0]) {
            return 0.0D;
        }
        for (int i = 1; i < points.size(); i++) {
            double[] low = points.get(i - 1);
            double[] high = points.get(i);
            if (armor <= high[0]) {
                return high[0] <= low[0] ? high[1] : low[1] + (high[1] - low[1]) * (armor - low[0]) / (high[0] - low[0]);
            }
        }
        return points.get(points.size() - 1)[1];
    }

    private static List<double[]> parse(List<? extends String> raw) {
        List<double[]> points = new ArrayList<>();
        for (String entry : raw) {
            int split = entry.indexOf('=');
            if (split > 0) {
                try {
                    points.add(new double[]{Double.parseDouble(entry.substring(0, split).trim()), Double.parseDouble(entry.substring(split + 1).trim())});
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return points;
    }
}
