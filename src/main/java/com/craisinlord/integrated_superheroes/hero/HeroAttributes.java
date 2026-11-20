package com.craisinlord.integrated_superheroes.hero;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicFixes;
import com.craisinlord.integrated_superheroes.suit.PymAbilities;
import com.craisinlord.integrated_superheroes.suit.SuitAbilities;
import com.craisinlord.integrated_superheroes.suit.SuitContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class HeroAttributes {
    private static final ResourceLocation HEALTH = legacyId("health_modifier");
    private static final ResourceLocation ATTACK = legacyId("attack_modifier");
    private static final ResourceLocation ARMOR = legacyId("armor_modifier");
    private static final ResourceLocation KNOCKBACK = legacyId("knockback_modifier");
    private static final ResourceLocation STEP_HEIGHT = legacyId("step_height_modifier");
    private static final ResourceLocation SPEED = legacyId("speed_modifier");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> WARNED = new HashSet<>();
    private static final Set<Attribute> SUIT_CAPPED = Set.of(Attributes.MAX_HEALTH.value(), Attributes.ARMOR.value(), Attributes.ARMOR_TOUGHNESS.value());

    private HeroAttributes() {
    }

    public static void apply(Player player, MarvelBalanceConfig.HeroStats stats) {
        Map<Attribute, Double> suit = suitBonuses(player);
        set(player, Attributes.MAX_HEALTH, HEALTH, beyondSuit(suit, Attributes.MAX_HEALTH.value(), stats.health.get()));
        set(player, Attributes.ATTACK_DAMAGE, ATTACK, 0.0D);
        set(player, Attributes.ARMOR, ARMOR, beyondSuit(suit, Attributes.ARMOR.value(), stats.armor.get()));
        set(player, Attributes.ATTACK_KNOCKBACK, KNOCKBACK, stats.attackKnockback.get());
        set(player, Attributes.STEP_HEIGHT, STEP_HEIGHT, stats.stepHeight.get());
        set(player, Attributes.MOVEMENT_SPEED, SPEED, stats.movementSpeed.get());
        syncExtras(player, stats, suit);
        syncStrength(player, stats.strength.get());
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static Map<Attribute, Double> suitBonuses(Player player) {
        Map<Attribute, Double> bonuses = new HashMap<>();
        if (!(player instanceof ServerPlayer serverPlayer) || !MarvelBalanceConfig.SUIT_HIGHEST_BONUS_ONLY.get() || !poweredSuit(serverPlayer)) {
            return bonuses;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            stack.getAttributeModifiers().forEach(slot, (attribute, modifier) -> {
                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE && SUIT_CAPPED.contains(attribute.value())) {
                    bonuses.merge(attribute.value(), modifier.amount(), Double::sum);
                }
            });
        }
        return bonuses;
    }

    private static boolean poweredSuit(ServerPlayer player) {
        if (!player.isSpectator()) {
            SuitContext context = SuitAbilities.context(player);
            if (context != null && !SuitAbilities.loadout(context.suitId()).isEmpty() || !PymAbilities.loadout(player).isEmpty()) {
                return true;
            }
        }
        return ModCompat.THEMATIC && ThematicFixes.suited(player);
    }

    private static double beyondSuit(Map<Attribute, Double> suit, Attribute attribute, double amount) {
        Double worn = suit.get(attribute);
        return worn == null ? amount : Math.max(0.0D, amount - Math.max(0.0D, worn));
    }

    public static void clear(Player player) {
        set(player, Attributes.MAX_HEALTH, HEALTH, 0.0D);
        set(player, Attributes.ATTACK_DAMAGE, ATTACK, 0.0D);
        set(player, Attributes.ARMOR, ARMOR, 0.0D);
        set(player, Attributes.ATTACK_KNOCKBACK, KNOCKBACK, 0.0D);
        set(player, Attributes.STEP_HEIGHT, STEP_HEIGHT, 0.0D);
        set(player, Attributes.MOVEMENT_SPEED, SPEED, 0.0D);
        syncExtras(player, null);
        syncStrength(player, 0);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void set(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (amount == 0.0D) {
            if (current != null) {
                instance.removeModifier(id);
            }
            return;
        }
        if (current != null && current.amount() == amount && current.operation() == AttributeModifier.Operation.ADD_VALUE) {
            return;
        }
        instance.removeModifier(id);
        instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    public static void syncStrength(Player player, int level) {
        if (player.level().isClientSide) {
            return;
        }
        MobEffectInstance current = player.getEffect(MobEffects.DAMAGE_BOOST);
        boolean ours = current != null && current.isInfiniteDuration() && current.isAmbient();
        int amplifier = level - 1;
        if (level <= 0) {
            if (ours) {
                player.removeEffect(MobEffects.DAMAGE_BOOST);
            }
            return;
        }
        if (current != null && (ours ? current.getAmplifier() == amplifier : current.getAmplifier() >= amplifier)) {
            return;
        }
        if (ours) {
            player.removeEffect(MobEffects.DAMAGE_BOOST);
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, true));
    }

    public static void syncExtras(Player player, MarvelBalanceConfig.HeroStats stats) {
        syncExtras(player, stats, Map.of());
    }

    private static void syncExtras(Player player, MarvelBalanceConfig.HeroStats stats, Map<Attribute, Double> suit) {
        Map<ResourceLocation, Extra> wanted = new HashMap<>();
        if (stats != null) {
            for (String entry : stats.extraAttributes.get()) {
                Extra extra = parse(entry);
                if (extra != null) {
                    wanted.merge(extra.id(), extra, (a, b) -> new Extra(a.id(), a.attribute(), a.amount() + b.amount(), a.operation()));
                }
            }
        }
        if (!suit.isEmpty()) {
            wanted.replaceAll((id, extra) -> extra.operation() == AttributeModifier.Operation.ADD_VALUE
                    ? new Extra(id, extra.attribute(), beyondSuit(suit, extra.attribute().value(), extra.amount()), extra.operation()) : extra);
            wanted.values().removeIf(extra -> extra.amount() == 0.0D);
        }

        Map<ResourceLocation, Holder<Attribute>> candidates = new HashMap<>();
        for (MarvelBalanceConfig.HeroStats any : MarvelBalanceConfig.ALL_HERO_STATS) {
            for (String entry : any.extraAttributes.get()) {
                Extra extra = parse(entry);
                if (extra != null) {
                    candidates.put(extra.id(), extra.attribute());
                }
            }
        }
        candidates.forEach((id, attribute) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                return;
            }
            AttributeModifier current = instance.getModifier(id);
            Extra extra = wanted.get(id);
            if (extra == null) {
                if (current != null) {
                    instance.removeModifier(id);
                }
                return;
            }
            if (current != null && current.amount() == extra.amount() && current.operation() == extra.operation()) {
                return;
            }
            instance.removeModifier(id);
            instance.addPermanentModifier(new AttributeModifier(id, extra.amount(), extra.operation()));
        });
    }

    private record Extra(ResourceLocation id, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
    }

    private static Extra parse(String entry) {
        String[] parts = entry.split(";");
        if (parts.length != 3) {
            warn(entry, "expected namespace:attribute;amount;OPERATION");
            return null;
        }
        ResourceLocation attributeId = ResourceLocation.tryParse(parts[0].trim());
        Optional<Holder.Reference<Attribute>> attribute = attributeId == null
                ? Optional.empty() : BuiltInRegistries.ATTRIBUTE.getHolder(attributeId);
        if (attribute.isEmpty()) {
            warn(entry, "unknown attribute");
            return null;
        }
        try {
            double amount = Double.parseDouble(parts[1].trim());
            AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(parts[2].trim().toUpperCase(Locale.ROOT));
            return new Extra(extraId(attributeId, operation), attribute.get(), amount, operation);
        } catch (IllegalArgumentException e) {
            warn(entry, "bad amount or operation");
            return null;
        }
    }

    private static ResourceLocation extraId(ResourceLocation attributeId, AttributeModifier.Operation operation) {
        return ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "hero_extra/" + attributeId.getNamespace()
                + "/" + attributeId.getPath() + "/" + operation.getSerializedName());
    }

    private static void warn(String entry, String reason) {
        if (WARNED.add(entry)) {
            LOGGER.warn("Ignoring Marvel extraAttributes entry '{}': {}", entry, reason);
        }
    }

    private static ResourceLocation legacyId(String path) {
        return ResourceLocation.fromNamespaceAndPath("marvellegacy", path);
    }
}
