package com.craisinlord.integrated_superheroes.compat.thematic;

import com.google.common.collect.MapMaker;
import bond.thematic.api.content.flight.FlightTrait;
import bond.thematic.api.content.movement.MovementModes;
import bond.thematic.api.offline.Offline;
import bond.thematic.api.registries.data.EntityComponents;
import bond.thematic.api.registries.effect.ThematicGroundedEffect;
import bond.thematic.api.registries.effect.ThematicStatusEffect;
import bond.thematic.api.registries.effect.ThematicStatusEffects;
import bond.thematic.api.util.Effects;
import bond.thematic.api.util.ThematicHelper;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ThematicFixes {
    private static final Set<Player> THEMATIC_FLIGHT = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
    private static final Set<String> OWN_NAMESPACES = Set.of("thematic", IntegratedSuperheroes.MODID);

    private ThematicFixes() {
    }

    public static boolean suited(Entity entity) {
        return entity instanceof LivingEntity living && ThematicHelper.getArmor(living) != null;
    }

    public static boolean keepAbilities(Player player, Abilities abilities) {
        if (!ThematicOptions.on(ThematicOptions.KEEP_OTHER_FLIGHT) || Offline.isOffline(player) || grounded(player)) {
            return false;
        }
        if (thematicFlyer(player)) {
            THEMATIC_FLIGHT.add(player);
            return false;
        }
        if (THEMATIC_FLIGHT.remove(player)) {
            return false;
        }
        if (!player.level().isClientSide()) {
            EntityComponents.SPEED.get(player).setFlying(abilities.flying);
        }
        return true;
    }

    public static boolean ownsAbilities(Player player) {
        return !ThematicOptions.on(ThematicOptions.KEEP_OTHER_FLIGHT) || Offline.isOffline(player) || grounded(player) || thematicFlyer(player);
    }

    private static boolean thematicFlyer(Player player) {
        return !MovementModes.declared(player, "flight").isEmpty() || FlightTrait.flyer(player) != null;
    }

    private static boolean grounded(Player player) {
        return Effects.has(player, ThematicStatusEffects.GROUNDED)
                || Effects.has(player, ThematicStatusEffects.REFLEX_SLOW)
                || ThematicGroundedEffect.isFullyGrounded(player)
                || (ThematicStatusEffects.FROZEN != null && Effects.has(player, ThematicStatusEffects.FROZEN))
                || (ThematicStatusEffects.SPEED_STUN != null && Effects.has(player, ThematicStatusEffects.SPEED_STUN));
    }

    public static boolean otherElytra(LivingEntity entity) {
        if (!ThematicOptions.on(ThematicOptions.OTHER_ELYTRAS)) {
            return false;
        }
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        return !chest.isEmpty() && chest.canElytraFly(entity);
    }

    public static boolean skipElytraTick(LivingEntity entity) {
        if (!ThematicOptions.on(ThematicOptions.OTHER_ELYTRAS)) {
            return false;
        }
        return !entity.isFallFlying() || (FlightTrait.glider(entity) == null && FlightTrait.flyer(entity) == null);
    }

    public static boolean thematicItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem().getClass().getName().startsWith("bond.thematic.")) {
            return true;
        }
        return "thematic".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    public static boolean broadcastEffect(MobEffectInstance effect) {
        return !ThematicOptions.on(ThematicOptions.TRIM_EFFECT_BROADCASTS)
                || (effect != null && effect.getEffect().value() instanceof ThematicStatusEffect);
    }

    public static Map<ResourceLocation, JsonObject> ownContent(Map<ResourceLocation, JsonObject> found, String folder, boolean log) {
        if (found == null || !ThematicOptions.on(ThematicOptions.OWN_CONTENT_ONLY)) {
            return found;
        }
        Map<ResourceLocation, JsonObject> kept = new LinkedHashMap<>();
        int dropped = 0;
        for (Map.Entry<ResourceLocation, JsonObject> entry : found.entrySet()) {
            if (OWN_NAMESPACES.contains(entry.getKey().getNamespace())) {
                kept.put(entry.getKey(), entry.getValue());
            } else {
                dropped++;
            }
        }
        if (dropped == 0) {
            return found;
        }
        if (log) {
            IntegratedSuperheroes.LOGGER.info("Ignored {} files in other mods' data/*/{} folders that Thematic would have loaded as its own content", dropped, folder);
        }
        return kept;
    }

    private static int droppedGunFiles;

    public static boolean foreignGunFile(ResourceLocation id) {
        if (id == null || OWN_NAMESPACES.contains(id.getNamespace()) || !ThematicOptions.on(ThematicOptions.OWN_CONTENT_ONLY)) {
            return false;
        }
        droppedGunFiles++;
        return true;
    }

    public static void reportGunFiles() {
        if (droppedGunFiles > 0) {
            IntegratedSuperheroes.LOGGER.info("Ignored {} files in other mods' data/*/guns and gun_effects folders that Thematic would have loaded as its own guns", droppedGunFiles);
            droppedGunFiles = 0;
        }
    }
}
