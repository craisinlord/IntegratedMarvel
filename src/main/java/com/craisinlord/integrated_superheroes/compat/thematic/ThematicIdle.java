package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.content.ability.runtime.StateFlags;
import com.google.common.collect.MapMaker;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import bond.thematic.api.content.bridge.WearableBridge;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import bond.thematic.api.registries.data.EntityComponents;
import bond.thematic.api.registries.data.ability.AbilityDataComponent;
import bond.thematic.api.registries.data.ability.PersistentDataComponent;
import bond.thematic.api.registries.effect.ThematicStatusEffects;
import bond.thematic.api.util.AbilityUtils;
import bond.thematic.api.util.Effects;
import bond.thematic.api.util.ThematicHelper;
import com.craisinlord.integrated_superheroes.mixin.ThematicAbilityDataAccessor;
import com.craisinlord.integrated_superheroes.mixin.ThematicPersistentDataAccessor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ThematicIdle {
    public static final String HELD_KEY = "held_until_tick";
    private static final String HELD_DOWN_SUFFIX = "HeldDown";
    private static volatile double heldUntil;
    private static final Map<String, Long> LONG_PERSISTENT_KEYS = new ConcurrentHashMap<>();

    private ThematicIdle() {
    }

    public static void abilityDataPut(Object key, Object value) {
        if (HELD_KEY.equals(key)) {
            double until = value instanceof NumericTag number ? number.getAsDouble() : Double.MAX_VALUE;
            synchronized (ThematicIdle.class) {
                heldUntil = Math.max(heldUntil, until);
            }
        }
    }

    public static void persistentDataPut(Object key, Object value) {
        if (value instanceof LongTag && key instanceof String name) {
            LONG_PERSISTENT_KEYS.merge(name, ((LongTag) value).getAsLong(), Math::max);
        }
    }

    public static boolean markLive(String key, long gameTime) {
        if (key == null) {
            return true;
        }
        Long until = LONG_PERSISTENT_KEYS.get(key);
        return until != null && until > gameTime;
    }

    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();

    public static boolean wearsNothing(LivingEntity entity) {
        for (EquipmentSlot slot : SLOTS) {
            if (!entity.getItemBySlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static final Map<ServerPlayer, TickFlags> FLAGS = new MapMaker().weakKeys().makeMap();

    private record TickFlags(long time, StateFlags.Flags flags) {
    }

    public static StateFlags.Flags flags(ServerPlayer player, Supplier<StateFlags.Flags> compute) {
        long time = player.serverLevel().getGameTime();
        TickFlags cached = FLAGS.get(player);
        if (cached != null && cached.time() == time) {
            return cached.flags();
        }
        StateFlags.Flags flags = compute.get();
        FLAGS.put(player, new TickFlags(time, flags));
        return flags;
    }

    public static boolean unsuited(LivingEntity entity) {
        return !(entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof CharacterItem);
    }

    public static boolean movementQuiet(LivingEntity entity) {
        if (entity instanceof Player player && player.getInventory() == null) {
            return false;
        }
        if (!unsuited(entity) || !notHeld(entity)) {
            return false;
        }
        if (!entity.getActiveEffects().isEmpty()
                && (Effects.has(entity, ThematicStatusEffects.SPEED_STUN) || ThematicHelper.isStunned(entity))) {
            return false;
        }
        if (entity instanceof Player player && player.isUsingItem()) {
            return false;
        }
        Iterable<ItemStack> slots = entity.getAllSlots();
        if (slots != null) {
            for (ItemStack stack : slots) {
                if (WearableBridge.isStunWearable(stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean notHeld(LivingEntity entity) {
        Level level = entity.level();
        if (level == null) {
            return true;
        }
        if (heldUntil <= level.getGameTime()) {
            return true;
        }
        return !AbilityUtils.isHeld(entity);
    }

    public static boolean noStoredState(LivingEntity entity) {
        AbilityDataComponent data = EntityComponents.ABILITY_DATA.getNullable(entity);
        if (data != null && !((ThematicAbilityDataAccessor) data).integratedSuperheroes$syncedData().isEmpty()) {
            return false;
        }
        PersistentDataComponent persistent = EntityComponents.PERSISTENT_DATA_COMPONENT.getNullable(entity);
        return persistent == null || ((ThematicPersistentDataAccessor) persistent).integratedSuperheroes$persistentData().isEmpty();
    }

    public static boolean noHeldDownFlags(LivingEntity entity) {
        if (EntityComponents.ABILITY_DATA == null) {
            return true;
        }
        AbilityDataComponent data = EntityComponents.ABILITY_DATA.getNullable(entity);
        if (data == null) {
            return true;
        }
        for (Map.Entry<String, Tag> entry : ((ThematicAbilityDataAccessor) data).integratedSuperheroes$syncedData().entrySet()) {
            String key = entry.getKey();
            if (key != null && key.endsWith(HELD_DOWN_SUFFIX) && entry.getValue() instanceof ByteTag flag && flag.getAsByte() == 1) {
                return false;
            }
        }
        return true;
    }
}
