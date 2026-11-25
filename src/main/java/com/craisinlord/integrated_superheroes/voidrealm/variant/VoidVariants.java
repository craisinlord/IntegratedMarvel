package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.boss.WitherstormEntity;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicVariants;
import com.craisinlord.integrated_superheroes.hero.skin.SerumSkin;
import com.craisinlord.integrated_superheroes.hero.skin.SerumSkins;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.HeroMoves;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.VariantMoves;
import com.craisinlord.integrated_superheroes.voidworld.VoidTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoidVariants {
    public static final String TAG = "integrated_superheroes_void_variant";
    public static final String BROOD_TAG = "integrated_superheroes_void_brood";
    public static final TagKey<EntityType<?>> LURES = TagKey.create(Registries.ENTITY_TYPE, IntegratedSuperheroes.id("void_variants"));

    private VoidVariants() {
    }

    public static void register(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, VariantConfig.SPEC, "integrated_superheroes-void.toml");
        VariantEntities.register(modBus);
        VariantTracker.register();
        if (FMLEnvironment.dist.isClient()) {
            com.craisinlord.integrated_superheroes.voidrealm.variant.client.VariantClient.register(modBus);
        }
    }

    public static boolean thematicHost(Mob mob) {
        return ModCompat.THEMATIC && ThematicVariants.isHost(mob);
    }

    public static boolean isVariant(Entity entity) {
        return entity != null && entity.getTags().contains(TAG);
    }

    public static boolean full(ServerLevel level) {
        return loaded(level).size() + VariantTears.pending(level) >= VariantConfig.MAX_LOADED.get();
    }

    public static List<Mob> loaded(ServerLevel level) {
        return VariantTracker.loaded(level);
    }

    public static VariantProfile profile(Entity entity) {
        return VariantTracker.profile(entity);
    }

    public static VariantAlignment alignment(Entity entity) {
        return VariantTracker.alignment(entity);
    }

    public static boolean allied(Entity mob, Entity other) {
        if (other == null || other == mob) {
            return false;
        }
        Entity self = summoner(mob);
        Entity them = summoner(other);
        if (self != mob || them != other) {
            if (self == them) {
                return true;
            }
            if (them == mob || self == other) {
                return true;
            }
            mob = self;
            other = them;
        }
        if (other instanceof WitherstormEntity) {
            return isVariant(mob);
        }
        VariantAlignment mine = alignment(mob);
        VariantAlignment theirs = alignment(other);
        if (mine == null || theirs == null) {
            return false;
        }
        return mine == theirs || !VariantConfig.ready() || !VariantConfig.FIGHT_EACH_OTHER.get();
    }

    private static Entity summoner(Entity entity) {
        if (entity instanceof com.craisinlord.integrated_superheroes.hero.SwarmSquirrel squirrel && squirrel.getOwner() != null
                && isVariant(squirrel.getOwner())) {
            return squirrel.getOwner();
        }
        LivingEntity pym = com.craisinlord.integrated_superheroes.suit.PymColony.ownerOf(entity);
        return pym != null && isVariant(pym) ? pym : entity;
    }

    public static Mob spawn(ServerLevel level, BlockPos pos, int tier) {
        VariantProfile profile = VariantPool.get(level.getServer()).roll(level.random, tier, null);
        return profile == null ? null : spawnProfile(level, Vec3.atBottomCenterOf(pos), profile);
    }

    public static List<Mob> spawnBrood(ServerLevel level, BlockPos pos, int count, int tier) {
        List<Mob> out = new ArrayList<>();
        int cap = VariantConfig.MAX_LOADED.get() + VariantConfig.BROOD_EXTRA.get();
        for (int i = 0; i < count && loaded(level).size() < cap; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = 2.0D + level.random.nextDouble() * 4.0D;
            BlockPos at = BlockPos.containing(pos.getX() + Math.cos(angle) * distance, pos.getY(), pos.getZ() + Math.sin(angle) * distance);
            Mob mob = spawn(level, at, tier);
            if (mob != null) {
                mob.addTag(BROOD_TAG);
                out.add(mob);
            }
        }
        return out;
    }

    public static String wornLook(VariantProfile profile) {
        return switch (profile.kind()) {
            case THEMATIC -> profile.look();
            case SERUM, VILTRUMITE -> VariantPool.costume(profile.kind(), profile.family(), profile.look());
            default -> "";
        };
    }

    public static Mob spawnProfile(ServerLevel level, Vec3 at, VariantProfile profile) {
        VariantPool pool = VariantPool.get(level.getServer());
        VariantFamily family = pool.family(profile.kind(), profile.family());
        if (family == null) {
            return null;
        }
        Mob mob = null;
        boolean thematicHost = false;
        if (family.entity()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(profile.look())).orElse(null);
            if (type != null && type.create(level) instanceof Mob created) {
                created.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
                created.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
                mob = created;
            }
        } else if (profile.kind() != VariantKind.MARVEL) {
            String look = wornLook(profile);
            if (ModCompat.THEMATIC && !look.isEmpty() && VariantPool.thematicItem(look) != null) {
                mob = ThematicVariants.create(level, at, look, 3 + VariantTiers.power(profile.tier()));
                thematicHost = mob != null;
            }
        }
        List<String> hostStyle = List.of();
        if (mob == null && !family.entity() && profile.kind() != VariantKind.THEMATIC) {
            String set = profile.kind() == VariantKind.MARVEL ? profile.family() : pool.data().marvelCostumes.getOrDefault(profile.family(), "");
            mob = VariantEntities.create(level, at, profile, set);
            hostStyle = profile.kind() == VariantKind.MARVEL ? marvelStyle(profile.family()) : List.of();
        }
        if (mob == null) {
            return null;
        }
        List<String> moves = new ArrayList<>();
        if (!family.entity()) {
            VariantGear.equip(mob, profile);
            int count = 2;
            List<String> style = hostStyle;
            List<String> thematicMoves = thematicHost && profile.kind() == VariantKind.THEMATIC
                    ? ThematicVariants.pickMoves(mob, Integer.MAX_VALUE, level.random) : List.of();
            if (thematicMoves.isEmpty()) {
                for (String move : HeroMoves.kitFor(profile)) {
                    if (!moves.contains(move)) {
                        moves.add(move);
                    }
                }
            }
            if (thematicHost) {
                ThematicVariants.unlock(mob, thematicMoves);
                moves.addAll(thematicMoves);
                style = ThematicVariants.style(wornLook(profile));
            }
            if (thematicMoves.isEmpty()) {
                for (String move : VariantMoves.fallbackFor(profile.kind(), style, count)) {
                    if (moves.size() < count && !moves.contains(move)) {
                        moves.add(move);
                    }
                }
            }
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                mob.setDropChance(slot, 0.0F);
            }
        }
        VariantProfile placed = profile.withMoves(moves);
        placed.write(mob);
        VoidTier.stamp(mob, placed.tier());
        mob.addTag(TAG);
        mob.setCustomName(name(placed));
        if (!level.addFreshEntity(mob)) {
            return null;
        }
        mob.setHealth(mob.getMaxHealth());
        return mob;
    }

    private static List<String> marvelStyle(String set) {
        if (set.startsWith("iron_man") || set.startsWith("war_machine") || set.startsWith("iron_patriot") || set.equals("thor")) {
            return List.of("flyer", "brawler");
        }
        return set.startsWith("ant_man") || set.equals("wasp") ? List.of("duelist", "utility") : List.of("duelist");
    }

    public static Component name(VariantProfile profile) {
        return Component.translatable("integrated_superheroes.variant.name", heroName(profile));
    }

    public static Component heroName(VariantProfile profile) {
        String fallback = titleCase(profile.look());
        switch (profile.kind()) {
            case THEMATIC -> {
                Item item = VariantPool.thematicItem(profile.look());
                return item == null ? Component.literal(fallback) : item.getDescription();
            }
            case SERUM, VILTRUMITE -> {
                if (profile.look().contains(":")) {
                    return entityName(profile.look(), fallback);
                }
                if (!profile.look().equals(profile.family())) {
                    SerumSkin skin = SerumSkins.byId(profile.look()).orElse(null);
                    if (skin != null) {
                        return Component.translatable(skin.nameKey());
                    }
                }
                return Component.translatableWithFallback("integrated_superheroes.variant.hero." + profile.family(), fallback);
            }
            case MARVEL -> {
                return Component.translatableWithFallback("integrated_superheroes.variant.hero." + profile.family(), titleCase(profile.family()));
            }
            default -> {
                return entityName(profile.look(), fallback);
            }
        }
    }

    private static Component entityName(String id, String fallback) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(id)).orElse(null);
        return type == null ? Component.literal(fallback) : type.getDescription();
    }

    private static String titleCase(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder out = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return out.toString();
    }
}
