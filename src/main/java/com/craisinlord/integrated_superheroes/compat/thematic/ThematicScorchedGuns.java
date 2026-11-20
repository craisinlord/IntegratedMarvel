package com.craisinlord.integrated_superheroes.compat.thematic;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.ScorchedGunsVillains;
import com.craisinlord.integrated_superheroes.compat.ScorchedGunsWeapons;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

public final class ThematicScorchedGuns {
    private static final String[][] ROLES = {
            {"grenade_launcher", "scguns:hammer_gl"},
            {"rocket_launcher", "scguns:dozier_rl"},
            {"auto_rifle", "scguns:kalaskah"},
            {"sniper", "scguns:m3_marksman"},
            {"rifle", "scguns:m3_carabine"},
            {"shotgun", "scguns:combat_shotgun"},
            {"smg", "scguns:greaser_smg"},
            {"lmg", "scguns:gattaler"},
            {"pistol", "scguns:defender_pistol"}
    };
    private static final String HEAVY_SHOTGUN = "scguns:super_shotgun";
    private static final String DEFAULT_GUN = "scguns:defender_pistol";
    private static final Map<String, String> BY_THEMATIC_GUN = Map.of(
            "thematic:desert_eagle", "scguns:defender_pistol",
            "thematic:alfreds_shotgun", "scguns:combat_shotgun",
            "thematic:m4a1", "scguns:m3_carabine",
            "thematic:tommygun", "scguns:greaser_smg",
            "thematic:minigun", "scguns:gattaler");

    private ThematicScorchedGuns() {
    }

    public static void register() {
        if (!ModCompat.SCORCHED_GUNS) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(ThematicScorchedGuns::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(ThematicScorchedGuns::onDetonate);
    }

    public static boolean enabled() {
        return ModCompat.SCORCHED_GUNS && ThematicOptions.on(ThematicOptions.CRIMINAL_SCORCHED_GUNS);
    }

    public static String gunFor(EntityType<?> type, String rolled) {
        if (!enabled()) {
            return rolled;
        }
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        String pick = null;
        for (String[] role : ROLES) {
            if (hasRole(path, role[0])) {
                pick = role[0].equals("shotgun") && path.contains("_heavy") ? HEAVY_SHOTGUN : role[1];
                break;
            }
        }
        if (pick == null && rolled != null && !rolled.isEmpty()) {
            pick = BY_THEMATIC_GUN.getOrDefault(rolled, rolled.startsWith("thematic:") ? DEFAULT_GUN : null);
        }
        if (pick == null) {
            return rolled;
        }
        ResourceLocation id = ResourceLocation.tryParse(pick);
        return id != null && BuiltInRegistries.ITEM.containsKey(id) ? pick : rolled;
    }

    private static boolean hasRole(String path, String role) {
        return path.equals(role) || path.startsWith(role + "_") || path.endsWith("_" + role) || path.contains("_" + role + "_");
    }

    public static boolean isGun(ItemStack stack) {
        return ModCompat.SCORCHED_GUNS && ScorchedGunsVillains.isGun(stack);
    }

    public static void fire(Mob shooter, ItemStack gun, LivingEntity target) {
        Vec3 at = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
        ScorchedGunsVillains.shoot(shooter, gun, at, 1.0F);
    }

    private static boolean criminalBullet(Entity direct) {
        return direct != null && ScorchedGunsWeapons.isBullet(direct)
                && ScorchedGunsWeapons.bulletShooter(direct) instanceof bond.thematic.api.content.criminal.DataCriminalEntity;
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity direct = event.getSource().getDirectEntity();
        if (!criminalBullet(direct)) {
            return;
        }
        LivingEntity shooter = ScorchedGunsWeapons.bulletShooter(direct);
        if (shooter != event.getEntity() && shooter.isAlliedTo(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void onDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide() || event.getLevel().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        if (criminalBullet(event.getExplosion().getDirectSourceEntity())) {
            event.getAffectedBlocks().clear();
        }
    }
}
