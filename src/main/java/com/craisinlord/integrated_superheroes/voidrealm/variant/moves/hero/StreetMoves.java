package com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.registry.HeroEntities;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.HeroStun;
import com.craisinlord.integrated_superheroes.hero.SwarmSquirrel;
import com.craisinlord.integrated_superheroes.registry.ModSounds;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.VariantMoves;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class StreetMoves {
    private static final DustParticleOptions CABLE = new DustParticleOptions(new Vector3f(0.25F, 0.25F, 0.28F), 0.6F);
    private static final DustParticleOptions RADAR = new DustParticleOptions(new Vector3f(0.85F, 0.1F, 0.1F), 1.0F);
    private static final int SWARM_CAP = 6;
    private static final int SWARM_LIFE = 240;

    private StreetMoves() {
    }

    public static void register(Map<String, VariantMoves.Move> map) {
        add(map, "billy_club_throw", 4.0D, 18.0D, StreetMoves::billyClub);
        add(map, "grapple_takedown", 5.0D, 16.0D, StreetMoves::grapple);
        add(map, "takedown", 0.0D, 3.5D, StreetMoves::takedown);
        add(map, "staff_sweep", 0.0D, 4.5D, StreetMoves::staffSweep);
        add(map, "radar_dodge", 0.0D, 6.0D, StreetMoves::radarDodge);
        add(map, "call_swarm", 0.0D, 18.0D, StreetMoves::callSwarm);
        add(map, "sic_em", 3.0D, 16.0D, StreetMoves::sicEm);
        add(map, "squirrel_pile", 0.0D, 12.0D, StreetMoves::squirrelPile);
    }

    private static void add(Map<String, VariantMoves.Move> map, String id, double min, double max, VariantMoves.Runner runner) {
        map.put(id, new VariantMoves.Move(id, min, max, runner));
    }

    private static ItemStack club() {
        return new ItemStack(HeroFx.item("thematic:billy_club", Items.STICK));
    }

    private static void billyClub(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.sound(level, mob.position(), SoundEvents.TRIDENT_THROW.value(), 0.8F, 1.4F);
        HeroKit.hurl(level, mob, target, club(), 1.2D, 2, 40.0F,
                (lvl, a, b) -> lvl.sendParticles(ParticleTypes.CRIT, b.x, b.y, b.z, 1, 0.05D, 0.05D, 0.05D, 0.0D),
                (victim, at) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.1D), at, 0.6D, 0.2D);
                    HeroKit.sound(level, victim.position(), SoundEvents.WOOD_HIT, 1.0F, 0.9F);
                    if (HeroAbilities.canControl(victim)) {
                        HeroStun.stun(victim, 15);
                    }
                },
                () -> HeroKit.sound(level, mob.position(), SoundEvents.ITEM_PICKUP, 0.8F, 0.8F));
    }

    private static void grapple(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 anchor = HeroKit.chest(target);
        HeroKit.line(level, HeroKit.hand(mob), anchor, CABLE, 0.3D);
        HeroKit.sound(level, mob.position(), SoundEvents.CROSSBOW_SHOOT, 0.8F, 1.3F);
        Vec3 pull = anchor.subtract(mob.position());
        Vec3 dir = pull.lengthSqr() < 1.0E-4D ? Vec3.ZERO : pull.normalize();
        mob.setDeltaMovement(dir.x * 1.6D, Math.max(0.35D, dir.y * 1.2D + 0.3D), dir.z * 1.6D);
        mob.hurtMarked = true;
        mob.fallDistance = 0.0F;
        VillainWorld.later(level, 9, () -> {
            if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.5D || !HeroKit.foe(mob, target)) {
                return;
            }
            mob.swing(InteractionHand.MAIN_HAND);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.5D), mob.position(), 1.2D, 0.45D);
            HeroKit.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.8F);
            Vec3 at = HeroKit.chest(target);
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.3D, 0.3D, 0.3D, 0.2D);
        });
    }

    private static void takedown(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        for (int i = 0; i < 3; i++) {
            int strike = i;
            VillainWorld.later(level, 2 + i * 4, () -> {
                if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.4D || !HeroKit.foe(mob, target)) {
                    return;
                }
                mob.swing(strike % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                target.invulnerableTime = 0;
                boolean last = strike == 2;
                HeroKit.hit(level, mob, target, HeroKit.damage(mob, last ? 1.3D : 0.7D), mob.position(), last ? 1.0D : 0.2D, last ? 0.3D : 0.05D);
                Vec3 at = HeroKit.chest(target);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.3D, 0.3D, 0.3D, 0.1D);
                HeroKit.sound(level, at, last ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
                if (last && HeroAbilities.canControl(target)) {
                    HeroStun.stun(target, 30);
                }
            });
        }
    }

    private static void staffSweep(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        Vec3 center = mob.position();
        HeroKit.sound(level, center, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.8F);
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x + Math.cos(angle) * 2.2D, center.y + 1.0D, center.z + Math.sin(angle) * 2.2D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        for (LivingEntity victim : HeroKit.victims(level, mob, center, 4.0D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.0D), center, 1.0D, 0.35D);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), mob);
        }
    }

    private static void radarDodge(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 spot = HeroKit.behind(target, 1.6D);
        Vec3 from = mob.position();
        for (int ring = 1; ring <= 3; ring++) {
            int size = ring;
            VillainWorld.later(level, ring * 2, () -> {
                for (int i = 0; i < 16; i++) {
                    double angle = Math.PI * 2.0D * i / 16.0D;
                    level.sendParticles(RADAR, from.x + Math.cos(angle) * size * 2.0D, from.y + 1.0D, from.z + Math.sin(angle) * size * 2.0D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            });
        }
        Vec3 dir = spot.subtract(from).multiply(1.0D, 0.0D, 1.0D);
        Vec3 flip = dir.lengthSqr() < 1.0E-4D ? Vec3.ZERO : dir.normalize();
        mob.setDeltaMovement(flip.x * 1.1D, 0.55D, flip.z * 1.1D);
        mob.hurtMarked = true;
        HeroKit.sound(level, from, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F, 1.2F);
        VillainWorld.later(level, 10, () -> {
            if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.5D || !HeroKit.foe(mob, target)) {
                return;
            }
            HeroKit.face(mob, target);
            mob.swing(InteractionHand.MAIN_HAND);
            target.invulnerableTime = 0;
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.3D), mob.position(), 0.8D, 0.25D);
        });
    }

    private static List<SwarmSquirrel> swarm(ServerLevel level, Mob mob) {
        return level.getEntitiesOfClass(SwarmSquirrel.class, mob.getBoundingBox().inflate(48.0D), squirrel -> squirrel.isAlive() && squirrel.summonedBy(mob));
    }

    private static BlockPos groundNear(ServerLevel level, Mob mob) {
        for (int attempt = 0; attempt < 8; attempt++) {
            BlockPos pos = mob.blockPosition().offset(level.random.nextInt(7) - 3, 0, level.random.nextInt(7) - 3);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos at = pos.above(dy);
                if (level.isEmptyBlock(at) && !level.isEmptyBlock(at.below())) {
                    return at;
                }
            }
        }
        return mob.blockPosition();
    }

    private static List<SwarmSquirrel> summon(ServerLevel level, Mob mob, LivingEntity target, int count) {
        List<SwarmSquirrel> out = new ArrayList<>(swarm(level, mob));
        float bite = HeroKit.damage(mob, 0.25D);
        while (out.size() < SWARM_CAP && count-- > 0) {
            SwarmSquirrel squirrel = HeroEntities.SWARM_SQUIRREL.get().create(level);
            if (squirrel == null) {
                break;
            }
            BlockPos ground = groundNear(level, mob);
            squirrel.moveTo(ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            squirrel.summonFor(mob, target, bite, 8.0D, SWARM_LIFE);
            if (!level.addFreshEntity(squirrel)) {
                continue;
            }
            BlockState below = level.getBlockState(ground.below());
            if (!below.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below), ground.getX() + 0.5D, ground.getY() + 0.1D,
                        ground.getZ() + 0.5D, 10, 0.25D, 0.1D, 0.25D, 0.1D);
            }
            squirrel.setDeltaMovement(level.random.nextGaussian() * 0.1D, 0.35D, level.random.nextGaussian() * 0.1D);
            out.add(squirrel);
        }
        return out;
    }

    private static void callSwarm(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.sound(level, mob.position(), ModSounds.SWARM_SQUIRREL_IDLE.get(), 1.2F, 1.0F);
        HeroKit.sound(level, mob.position(), ModSounds.SWARM_SQUIRREL_BEG.get(), 1.0F, 1.3F);
        summon(level, mob, target, 4);
    }

    private static void sicEm(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        List<SwarmSquirrel> squirrels = summon(level, mob, target, 2);
        for (SwarmSquirrel squirrel : squirrels) {
            squirrel.focus(target);
            Vec3 leap = target.position().subtract(squirrel.position());
            Vec3 dir = leap.lengthSqr() < 1.0E-4D ? Vec3.ZERO : leap.normalize();
            squirrel.setDeltaMovement(dir.x * 0.9D, 0.45D, dir.z * 0.9D);
        }
        HeroKit.sound(level, target.position(), ModSounds.SWARM_SQUIRREL_BEG.get(), 1.3F, 1.2F);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), mob);
    }

    private static void squirrelPile(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        List<SwarmSquirrel> squirrels = summon(level, mob, target, 3);
        squirrels.sort((a, b) -> Double.compare(a.distanceToSqr(target), b.distanceToSqr(target)));
        float bite = HeroKit.damage(mob, 0.3D);
        for (int i = 0; i < squirrels.size(); i++) {
            squirrels.get(i).pile(target, 50, bite, i);
        }
        if (!squirrels.isEmpty() && HeroAbilities.canControl(target)) {
            HeroStun.stun(target, 40);
        }
        HeroKit.sound(level, target.position(), ModSounds.SWARM_SQUIRREL_BEG.get(), 1.5F, 1.1F);
        HeroKit.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);
        AABB box = target.getBoundingBox();
        level.sendParticles(ParticleTypes.POOF, box.getCenter().x, box.getCenter().y, box.getCenter().z, 8, 0.4D, 0.4D, 0.4D, 0.02D);
    }
}
