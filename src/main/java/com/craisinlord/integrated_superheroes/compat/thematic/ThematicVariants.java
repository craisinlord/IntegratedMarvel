package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.ability.AbilityCapability;
import bond.thematic.api.ability.ThematicAbility;
import bond.thematic.api.content.ability.part.ComponentDef;
import bond.thematic.api.content.ability.runtime.ComposedAbility;
import bond.thematic.api.content.entity.body.DataMobEntity;
import bond.thematic.api.registries.armors.armor.ArmorRegistry;
import bond.thematic.api.registries.armors.armor.CharacterAltItem;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import bond.thematic.api.registries.data.stats.DisabledSuits;
import bond.thematic.mod.abilities.AbilityRegistry;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.mixin.ThematicComponentDefAccessor;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantAlignment;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantFamily;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantKind;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantPoolData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class ThematicVariants {
    public static final ResourceLocation HOST = ResourceLocation.fromNamespaceAndPath("thematic", "void_variant");
    private static final Set<ComponentDef> ENABLED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static volatile Set<String> allowlist = Set.of();

    private ThematicVariants() {
    }

    public static boolean isHost(Entity entity) {
        return entity instanceof DataMobEntity && HOST.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
    }

    public static boolean hostRegistered() {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(HOST);
    }

    private static int gate(int suitTier) {
        return suitTier >= 8 ? 3 : suitTier == 7 ? 2 : suitTier >= 6 ? 1 : 0;
    }

    private static boolean active(ThematicAbility ability) {
        String key = ability.getKeybind();
        return key != null && (key.contains("ability_") || key.contains("ultimate"));
    }

    private static boolean usable(CharacterItem armor) {
        return !armor.isWip() && !DisabledSuits.isDisabled(armor);
    }

    private static ComponentDef def(ThematicAbility ability) {
        return ability instanceof ComposedAbility composed ? composed.def() : null;
    }

    private static boolean native_(ThematicAbility ability) {
        ComponentDef def = def(ability);
        return AbilityCapability.of(ability, AbilityCapability.MOB_USABLE) && (def == null || !ENABLED.contains(def));
    }

    private static boolean mobUsable(ThematicAbility ability) {
        return AbilityCapability.of(ability, AbilityCapability.MOB_USABLE);
    }

    public static List<VariantFamily> families(VariantPoolData data) {
        Map<CharacterItem, List<String>> alts = new IdentityHashMap<>();
        for (CharacterAltItem alt : ArmorRegistry.getAlts()) {
            CharacterItem main = alt.getMainArmor();
            if (main != null && usable(alt)) {
                alts.computeIfAbsent(main, key -> new ArrayList<>()).add(alt.getArmorId());
            }
        }
        List<VariantFamily> out = new ArrayList<>();
        for (CharacterItem armor : ArmorRegistry.getArmors()) {
            if (armor instanceof CharacterAltItem || !usable(armor)) {
                continue;
            }
            String id = armor.getArmorId();
            String collection = armor.getCollectionType() == null ? "" : armor.getCollectionType().getID();
            if (data.excludedCollections.contains(collection)) {
                continue;
            }
            List<String> looks = new ArrayList<>();
            looks.add(id);
            looks.addAll(alts.getOrDefault(armor, List.of()));
            VariantAlignment alignment = id.startsWith("villain_") || data.villainCollections.contains(collection)
                    ? VariantAlignment.VILLAIN : VariantAlignment.HERO;
            String key = "thematic:" + id;
            out.add(new VariantFamily(VariantKind.THEMATIC, id, looks, data.gate(key, gate(armor.getTier())), alignment,
                    data.weight(key), false, style(armor)));
        }
        return out;
    }

    private static String style(CharacterItem armor) {
        boolean anyActive = false;
        boolean anyNative = false;
        boolean anyAllowed = false;
        for (ThematicAbility ability : armor.getAbilities()) {
            if (ability == null) {
                continue;
            }
            if (native_(ability)) {
                anyNative = true;
            } else if (active(ability) && mobUsable(ability)) {
                anyAllowed = true;
            }
            anyActive |= active(ability);
        }
        return anyNative ? "native" : anyAllowed ? "allowlisted" : anyActive ? "fallback" : "stat_only";
    }

    public static synchronized void applyAllowlist(VariantPoolData data) {
        for (ComponentDef def : ENABLED) {
            ((ThematicComponentDefAccessor) (Object) def).integratedSuperheroes$setMob(null);
        }
        ENABLED.clear();
        ThematicFlight.clearCache();
        ThematicFlight.register();
        allowlist = Set.copyOf(data.mobMoves.keySet());
        List<ThematicAbility> abilities = new ArrayList<>(AbilityRegistry.ABILITY_MAP.values());
        for (CharacterItem armor : ArmorRegistry.getArmors()) {
            abilities.addAll(armor.getAbilities());
        }
        for (CharacterAltItem alt : ArmorRegistry.getAlts()) {
            abilities.addAll(alt.getAbilities());
        }
        int count = 0;
        Map<ThematicMobMoves.Kind, Integer> kinds = new EnumMap<>(ThematicMobMoves.Kind.class);
        Set<String> enabledIds = new TreeSet<>();
        Set<String> skipped = new TreeSet<>();
        for (ThematicAbility ability : abilities) {
            if (ability == null) {
                continue;
            }
            VariantPoolData.MobMove move = data.mobMoves.get(ability.getId());
            ComponentDef def = def(ability);
            if (move == null || def == null || def.mob() != null || def.key() == null) {
                continue;
            }
            ThematicMobMoves.Built built;
            try {
                built = ThematicMobMoves.build(ability, def, move);
            } catch (RuntimeException error) {
                IntegratedSuperheroes.LOGGER.warn("Void Variants: could not mob-enable {}", ability.getId(), error);
                built = null;
            }
            if (built == null) {
                skipped.add(ability.getId());
                continue;
            }
            ((ThematicComponentDefAccessor) (Object) def).integratedSuperheroes$setMob(built.part());
            ENABLED.add(def);
            kinds.merge(built.kind(), 1, Integer::sum);
            enabledIds.add(ability.getId());
            count++;
        }
        skipped.removeAll(enabledIds);
        IntegratedSuperheroes.LOGGER.info("Void Variants: mob-enabled {} ability instances ({}) for {} of {} allowlisted ids",
                count, kinds, enabledIds.size(), data.mobMoves.size());
        if (!skipped.isEmpty()) {
            IntegratedSuperheroes.LOGGER.info("Void Variants: allowlisted but not usable by a mob: {}", skipped);
        }
    }

    public static Item lookItem(String look) {
        ResourceLocation key = ResourceLocation.tryParse(look.contains(":") ? look : "thematic:" + look);
        return key == null ? null : BuiltInRegistries.ITEM.getOptional(key).orElse(null);
    }

    public static Mob create(ServerLevel level, Vec3 at, String look, int iv) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(HOST).orElse(null);
        if (type == null || !(type.create(level) instanceof DataMobEntity mob)) {
            return null;
        }
        mob.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        if (!wear(mob, look, Mth.clamp(iv, 1, 5))) {
            mob.discard();
            return null;
        }
        ThematicFlight.register();
        return mob;
    }

    private static boolean wear(DataMobEntity mob, String look, int iv) {
        Item item = lookItem(look);
        CharacterItem base;
        if (item instanceof CharacterAltItem alt && alt.getMainArmor() != null) {
            base = alt.getMainArmor();
        } else if (item instanceof CharacterItem suit && !(item instanceof CharacterAltItem)) {
            base = suit;
        } else {
            return false;
        }
        if (!mob.equipSuit(base.getArmorId(), iv)) {
            return false;
        }
        showLook(mob, item);
        return true;
    }

    private static void showLook(Mob mob, Item item) {
        ItemStack chest = mob.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || chest.is(item)) {
            return;
        }
        ItemStack shown = new ItemStack(item);
        shown.applyComponents(chest.getComponentsPatch());
        mob.setItemSlot(EquipmentSlot.CHEST, shown);
        mob.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    public static void restore(Mob mob, String look) {
        Item item = lookItem(look);
        if (item instanceof CharacterItem && isHost(mob)) {
            showLook(mob, item);
        }
    }

    public static List<String> pickMoves(Mob mob, int count, RandomSource random) {
        CharacterItem suit = mob.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof CharacterItem worn ? worn : null;
        List<String> natives = new ArrayList<>();
        List<String> allowed = new ArrayList<>();
        if (suit != null) {
            for (ThematicAbility ability : suit.getAbilities()) {
                if (ability == null || !mobUsable(ability)) {
                    continue;
                }
                (native_(ability) ? natives : allowed).add(ability.getId());
            }
        }
        Util.shuffle(natives, random);
        Util.shuffle(allowed, random);
        List<String> picked = new ArrayList<>();
        for (String id : natives) {
            if (picked.size() < count && !picked.contains(id)) {
                picked.add(id);
            }
        }
        for (String id : allowed) {
            if (picked.size() < count && !picked.contains(id)) {
                picked.add(id);
            }
        }
        return picked;
    }

    public static void unlock(Mob mob, List<String> abilityIds) {
        if (mob instanceof DataMobEntity data) {
            data.setUnlockedAbilities(abilityIds);
        }
    }

    public static List<String> style(String look) {
        List<String> out = new ArrayList<>();
        Item item = lookItem(look);
        if (!(item instanceof CharacterItem suit)) {
            return out;
        }
        for (CharacterItem.FIGHTING_TYPE type : suit.fightingTypes) {
            out.add(type.name().toLowerCase(Locale.ROOT));
        }
        CharacterItem base = suit instanceof CharacterAltItem alt && alt.getMainArmor() != null ? alt.getMainArmor() : suit;
        if (ThematicFlight.flyer(base)) {
            out.add("flyer");
        }
        return out;
    }

    public static boolean busy(Mob mob) {
        return mob instanceof DataMobEntity data && data.abilityController().isBusy();
    }

    public static LivingEntity target(Mob mob) {
        return mob instanceof DataMobEntity data ? data.attackTarget() : mob.getTarget();
    }

    public static void setTarget(Mob mob, LivingEntity target) {
        if (mob instanceof DataMobEntity data) {
            if (target == null) {
                data.clearAttackTargetMemory();
                data.setTarget(null);
            } else {
                data.setAttackTarget(target);
            }
        } else {
            mob.setTarget(target);
        }
    }

    public static List<String> poolLines(Map<String, VariantFamily> thematic) {
        int nativeCount = 0;
        int allowed = 0;
        int fallback = 0;
        int statOnly = 0;
        int looks = 0;
        for (VariantFamily family : thematic.values()) {
            looks += family.looks().size();
            switch (family.note()) {
                case "native" -> nativeCount++;
                case "allowlisted" -> allowed++;
                case "fallback" -> fallback++;
                default -> statOnly++;
            }
        }
        return List.of("thematic: " + thematic.size() + " families, " + looks + " looks; " + nativeCount + " with native mob moves, "
                + allowed + " allowlisted, " + fallback + " fallback only, " + statOnly + " stat-only; allowlist " + allowlist.size() + " ids");
    }

    public static List<String> audit(ServerLevel level, Vec3 origin, Set<String> ids) {
        List<String> lines = new ArrayList<>();
        int index = 0;
        for (String id : ids) {
            CharacterItem carrier = null;
            for (CharacterItem armor : ArmorRegistry.getArmors()) {
                if (!(armor instanceof CharacterAltItem) && armor.getAbility(id) != null && !armor.isWip()) {
                    carrier = armor;
                    break;
                }
            }
            if (carrier == null) {
                lines.add(id + ": no suit carries it");
                continue;
            }
            Vec3 at = origin.add(3.0D * index++, 0.0D, 4.0D);
            Mob host = create(level, at, carrier.getArmorId(), 3);
            Zombie dummy = EntityType.ZOMBIE.create(level);
            if (host == null || dummy == null) {
                lines.add(id + ": could not spawn a test Variant in " + carrier.getArmorId());
                continue;
            }
            dummy.moveTo(at.x, at.y, at.z + 6.0D, 180.0F, 0.0F);
            dummy.setNoAi(true);
            dummy.setPersistenceRequired();
            host.setPersistenceRequired();
            level.addFreshEntity(host);
            level.addFreshEntity(dummy);
            DataMobEntity data = (DataMobEntity) host;
            data.setUnlockedAbilities(List.of(id));
            String result;
            try {
                boolean ability = data.abilityController().unlocked().contains(id) && mobUsable(carrier.getAbility(id));
                boolean started = ability && data.abilityController().useNow(id, dummy);
                result = !ability ? "not mob-usable" : !data.abilityController().unlocked().contains(id) ? "threw (see log)"
                        : started ? "fired" : "declined";
            } catch (RuntimeException error) {
                IntegratedSuperheroes.LOGGER.error("Void Variants audit: {} threw", id, error);
                result = "threw: " + error.getClass().getSimpleName();
            }
            lines.add(id + " (" + carrier.getArmorId() + "): " + result);
            IntegratedSuperheroes.LOGGER.info("Void Variants audit: {} on {}: {}", id, carrier.getArmorId(), result);
            VillainWorld.later(level, 80, () -> {
                host.discard();
                dummy.discard();
            });
        }
        return lines;
    }

    private static final class Util {
        static <T> void shuffle(List<T> list, RandomSource random) {
            for (int i = list.size() - 1; i > 0; i--) {
                Collections.swap(list, i, random.nextInt(i + 1));
            }
        }
    }
}
