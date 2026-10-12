package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.redgitreds.infinitygauntlet.procedures.MindInfectionOnEffectActiveTickProcedure", remap = false)
public abstract class InfinityGauntletMindInfectionMixin {
    @Unique
    private static final String INTEGRATED_SUPERHEROES$TEAM = "Mind";
    @Unique
    private static final DustParticleOptions INTEGRATED_SUPERHEROES$DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 0.0F), 1.0F);

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$executeDirectly(LevelAccessor world, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ci.cancel();
        if (entity == null || entity instanceof Player || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(INTEGRATED_SUPERHEROES$TEAM);
        if (team == null) {
            team = scoreboard.addPlayerTeam(INTEGRATED_SUPERHEROES$TEAM);
        }
        if (team.isAllowFriendlyFire()) {
            team.setAllowFriendlyFire(false);
        }
        if (entity.getTeam() != team) {
            scoreboard.addPlayerToTeam(entity.getScoreboardName(), team);
        }
        if (Math.random() > 0.5D && Math.random() > 0.5D && Math.random() > 0.5D && Math.random() < 0.1D
                && entity instanceof LivingEntity living
                && !world.getEntitiesOfClass(Player.class, new AABB(x, y, z, x, y, z).inflate(2.0D), player -> true).isEmpty()) {
            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2, false, false));
        }
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, INTEGRATED_SUPERHEROES$DUST, true, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 5, 0.5D, 0.5D, 0.5D, 0.0D);
        }
    }
}
