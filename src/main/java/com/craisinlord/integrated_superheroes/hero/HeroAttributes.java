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
import net.minecraft.world.entity.player.Player;
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

    private HeroAttributes() {
    }

    public static void apply(Player player, MarvelBalanceConfig.HeroStats stats) {
        set(player, Attributes.MAX_HEALTH, HEALTH, stats.health.get());
        set(player, Attributes.ATTACK_DAMAGE, ATTACK, 0.0D);
        set(player, Attributes.ARMOR, ARMOR, stats.armor.get());
        set(player, Attributes.ATTACK_KNOCKBACK, KNOCKBACK, stats.attackKnockback.get());
        set(player, Attributes.STEP_HEIGHT, STEP_HEIGHT, stats.stepHeight.get());
        set(player, Attributes.MOVEMENT_SPEED, SPEED, stats.movementSpeed.get());
        syncExtras(player, stats);
        syncStrength(player, stats.strength.get());
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
        Map<ResourceLocation, Extra> wanted = new HashMap<>();
        if (stats != null) {
            for (String entry : stats.extraAttributes.get()) {
                Extra extra = parse(entry);
                if (extra != null) {
                    wanted.merge(extra.id(), extra, (a, b) -> new Extra(a.id(), a.attribute(), a.amount() + b.amount(), a.operation()));
                }
            }
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
