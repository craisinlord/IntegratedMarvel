package com.craisinlord.integrated_marvel.hero;

import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class InvisibleWomanPower {
    public static final String ID = "invisibility_sue_storm";
    public static final String INVISIBLE_STATE = "integrated_marvel_invisible";
    public static final String BUBBLE_STATE = "integrated_marvel_bubble";
    private static final String VANISH = "vanish";
    private static final String FORCE_BUBBLE = "force_bubble";
    private static final DustParticleOptions FORCE = new DustParticleOptions(new Vector3f(0.55F, 0.8F, 1.0F), 1.0F);
    private static final Map<UUID, Long> BUBBLE_UNTIL = new HashMap<>();

    private InvisibleWomanPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Invisibility (Sue Storm)",
                syringe,
                MarvelBalanceConfig.SUE,
                List.of(),
                List.of(
                        new HeroPower.Ability(VANISH, "Vanish", InvisibleWomanPower::vanish),
                        new HeroPower.Ability("force_field", "Force Field", InvisibleWomanPower::forceField),
                        new HeroPower.Ability(FORCE_BUBBLE, "Force Bubble", InvisibleWomanPower::forceBubble),
                        new HeroPower.Ability("force_blast", "Force Blast", InvisibleWomanPower::forceBlast)
                ),
                List.of(),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (isInvisible(player) && player.tickCount % 10 == 0) {
                            loseMobs(player);
                        }
                        if (hasBubble(player)) {
                            tickBubble(player);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        PlayerData data = HeroPowers.data(player);
                        data.removeState(INVISIBLE_STATE);
                        data.removeState(BUBBLE_STATE);
                        BUBBLE_UNTIL.remove(player.getUUID());
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (hasBubble(player) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                            event.setAmount(event.getAmount() * MarvelBalanceConfig.BUBBLE_DAMAGE_MULTIPLIER.get().floatValue());
                            Vec3 at = player.position().add(0.0D, 1.0D, 0.0D);
                            player.serverLevel().sendParticles(FORCE, at.x, at.y, at.z, 10, 0.8D, 0.8D, 0.8D, 0.0D);
                            HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_HIT, 1.0F, 1.4F);
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        reveal(player);
                    }
                }
        );
    }

    public static boolean isInvisible(Player player) {
        return player.hasData(com.stxllarplanes.marvellegacy.init.AttachmentsRegistry.PLAYER_DATA.get())
                && HeroPowers.data(player).hasState(INVISIBLE_STATE);
    }

    private static boolean hasBubble(Player player) {
        return HeroPowers.data(player).hasState(BUBBLE_STATE);
    }

    public static void reveal(ServerPlayer player) {
        PlayerData data = HeroPowers.data(player);
        if (!data.hasState(INVISIBLE_STATE)) {
            return;
        }
        data.removeState(INVISIBLE_STATE);
        PlayerData.sync(player);
        HeroAbilities.setCooldown(player, VANISH, MarvelBalanceConfig.VANISH_COOLDOWN.get());
        HeroFx.play(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.3F);
    }

    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && isInvisible(player)) {
            reveal(player);
        }
    }

    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player player && isInvisible(player)) {
            event.setCanceled(true);
        }
    }

    private static void loseMobs(ServerPlayer player) {
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0D),
                mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        for (LivingEntity living : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(48.0D),
                living -> living.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                        && living.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(target -> target == player).isPresent())) {
            living.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    private static double vanish(ServerPlayer player, int slot) {
        PlayerData data = HeroPowers.data(player);
        if (data.hasState(INVISIBLE_STATE)) {
            reveal(player);
            return MarvelBalanceConfig.VANISH_COOLDOWN.get();
        }
        data.addState(INVISIBLE_STATE);
        PlayerData.sync(player);
        loseMobs(player);
        HeroFx.play(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 0.7F);
        return 0.5D;
    }

    private static double forceField(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            flat = Vec3.directionFromRotation(0.0F, player.getYRot());
        }
        flat = flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x);
        Vec3 origin = player.position().add(flat.scale(3.0D));
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        int ticks = MarvelBalanceConfig.FORCE_FIELD_TICKS.get();
        int placed = 0;
        for (int w = -2; w <= 2; w++) {
            for (int h = 0; h < 3; h++) {
                Vec3 at = origin.add(side.scale(w)).add(0.0D, h, 0.0D);
                BlockPos pos = BlockPos.containing(at);
                if (level.getBlockState(pos).canBeReplaced() && level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos)).isEmpty()
                        && TempBlocks.place(level, player, pos, glass, ticks)) {
                    placed++;
                }
            }
        }
        if (placed == 0) {
            return 0.0D;
        }
        level.sendParticles(FORCE, origin.x, origin.y + 1.5D, origin.z, 30, 1.5D, 1.0D, 1.5D, 0.0D);
        HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
        HeroAnimations.play(player, "web_shoot_both");
        return MarvelBalanceConfig.FORCE_FIELD_COOLDOWN.get();
    }

    private static double forceBubble(ServerPlayer player, int slot) {
        PlayerData data = HeroPowers.data(player);
        if (data.hasState(BUBBLE_STATE)) {
            popBubble(player);
            return MarvelBalanceConfig.BUBBLE_COOLDOWN.get();
        }
        data.addState(BUBBLE_STATE);
        PlayerData.sync(player);
        BUBBLE_UNTIL.put(player.getUUID(), player.serverLevel().getGameTime() + (long) (MarvelBalanceConfig.BUBBLE_SECONDS.get() * 20.0D));
        HeroFx.play(player, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.6F);
        return 0.5D;
    }

    private static void popBubble(ServerPlayer player) {
        HeroPowers.data(player).removeState(BUBBLE_STATE);
        PlayerData.sync(player);
        BUBBLE_UNTIL.remove(player.getUUID());
        HeroFx.play(player, SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.6F);
    }

    private static void tickBubble(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Long until = BUBBLE_UNTIL.get(player.getUUID());
        if (until == null || level.getGameTime() >= until) {
            popBubble(player);
            HeroAbilities.setCooldown(player, FORCE_BUBBLE, MarvelBalanceConfig.BUBBLE_COOLDOWN.get());
            return;
        }
        double radius = MarvelBalanceConfig.BUBBLE_RADIUS.get();
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(radius + 1.0D))) {
            Vec3 offset = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D).subtract(center);
            double distance = offset.length();
            if (distance > radius + 0.5D || distance < 1.0E-3D) {
                continue;
            }
            Vec3 out = offset.normalize();
            if (entity instanceof Projectile projectile && projectile.getOwner() != player) {
                Vec3 motion = projectile.getDeltaMovement();
                if (motion.dot(out) < 0.0D) {
                    projectile.setDeltaMovement(motion.subtract(out.scale(2.0D * motion.dot(out))).scale(0.6D));
                    projectile.hurtMarked = true;
                    level.sendParticles(FORCE, entity.getX(), entity.getY(), entity.getZ(), 4, 0.1D, 0.1D, 0.1D, 0.0D);
                }
            } else if (entity instanceof LivingEntity living && living.isAlive()) {
                living.setDeltaMovement(out.x * 0.6D, 0.2D, out.z * 0.6D);
                living.hurtMarked = true;
            }
        }
        if (player.tickCount % 3 == 0) {
            for (int i = 0; i < 14; i++) {
                double theta = level.random.nextDouble() * Math.PI * 2.0D;
                double phi = Math.acos(2.0D * level.random.nextDouble() - 1.0D);
                double x = Math.sin(phi) * Math.cos(theta) * radius;
                double y = Math.cos(phi) * radius;
                double z = Math.sin(phi) * Math.sin(theta) * radius;
                level.sendParticles(FORCE, center.x + x, center.y + y, center.z + z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    private static double forceBlast(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        double range = MarvelBalanceConfig.FORCE_BLAST_RANGE.get();
        float damage = (float) (HeroPowers.powerDamage(player) * MarvelBalanceConfig.FORCE_BLAST_DAMAGE_MULTIPLIER.get());
        Vec3 look = player.getLookAngle();
        for (LivingEntity target : HeroAbilities.cone(player, range, 25.0D)) {
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().playerAttack(player), damage));
            target.setDeltaMovement(look.x * 2.2D, 0.6D, look.z * 2.2D);
            target.hurtMarked = true;
        }
        Vec3 start = player.getEyePosition().subtract(0.0D, 0.3D, 0.0D);
        for (int i = 1; i <= (int) range; i++) {
            Vec3 at = start.add(look.scale(i));
            double spread = i * 0.15D;
            level.sendParticles(FORCE, at.x, at.y, at.z, 4, spread, spread, spread, 0.0D);
        }
        HeroFx.play(player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.0F, 1.2F);
        HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.8F);
        HeroAnimations.play(player, "web_shoot_both");
        reveal(player);
        return MarvelBalanceConfig.FORCE_BLAST_COOLDOWN.get();
    }
}
