package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.api.RogueAbsorption;
import com.craisinlord.integrated_superheroes.api.RogueMemory;
import com.craisinlord.integrated_superheroes.hero.skin.SerumCostumes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class ProfessorXPower {
    public static final String ID = "telepathy_charles_xavier";
    public static final String LINK = "professor_x_link";
    public static final String CEREBRO = "professor_x_cerebro";
    public static final String ASTRAL = "professor_x_astral_projection";
    public static final HeroPower.Toggle TETHERS = new HeroPower.Toggle("integrated_superheroes_off_psychic_tethers", "Link Tethers",
            () -> Items.LEAD);
    private static final int MIND_COLOR = 0xB57CFF;
    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();
    private static RogueMemory rogue;
    private static boolean registered;

    private record Channel(UUID target, long start, long end) {
    }

    private ProfessorXPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            PsychicLinks.register();
            AstralProjection.register();
            NeoForge.EVENT_BUS.addListener(ProfessorXPower::onLogout);
            NeoForge.EVENT_BUS.addListener(ProfessorXPower::onChangeDimension);
            NeoForge.EVENT_BUS.addListener(ProfessorXPower::onStopping);
            SerumCostumes.register(ID, "thematic:prof_x");
            RogueAbsorption.remember(rogueMemory());
        }
        return new HeroPower(
                ID,
                "Telepathy (Professor X)",
                syringe,
                ProfessorXConfig.STATS,
                List.of(),
                List.of(
                        new HeroPower.Ability(LINK, "Link", ProfessorXPower::link),
                        new HeroPower.Ability(CEREBRO, "Cerebro", ProfessorXPower::cerebro),
                        new HeroPower.Ability(ASTRAL, "Astral Projection", ProfessorXPower::astral),
                        HeroToggles.WHEEL
                ),
                List.of(TETHERS),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        tickChannel(player);
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        clear(player);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        Channel channel = CHANNELS.get(player.getUUID());
                        if (channel != null && event.getAmount() >= ProfessorXConfig.CHANNEL_BREAK_DAMAGE.get()) {
                            endChannel(player);
                            player.displayClientMessage(Component.literal("§7The hit knocks your focus loose."), true);
                            HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_BREAK, 0.8F, 0.8F);
                        }
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        clear(player);
                    }
                }
        );
    }

    public static boolean isXavier(Player player) {
        return SerumKit.is(player, ID);
    }

    public static boolean tethersOn(ServerPlayer player) {
        return HeroToggles.isOn(player, TETHERS);
    }

    private static ParticleOptions glow(ServerPlayer player, float size) {
        return SerumKit.glow(player, MIND_COLOR, size);
    }

    private static double link(ServerPlayer player, int slot) {
        if (CHANNELS.containsKey(player.getUUID())) {
            endChannel(player);
            player.displayClientMessage(Component.literal("§7You let the thought go."), true);
            return 0.0D;
        }
        if (player.isShiftKeyDown() && PsychicLinks.count(player, false) > 0) {
            PsychicLinks.releaseAll(player);
            player.displayClientMessage(Component.literal("§7You let them go."), true);
            HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_BREAK, 0.6F, 1.4F);
            return 0.5D;
        }
        LivingEntity target = sighted(player, ProfessorXConfig.LINK_RANGE.get());
        if (target == null) {
            player.displayClientMessage(Component.literal("§7Look at a hostile creature to reach its mind."), true);
            return 0.0D;
        }
        if (!(target instanceof Mob) || !PsychicLinks.canLink(player, target)) {
            player.displayClientMessage(Component.literal("§7That mind won't open to you."), true);
            return 0.0D;
        }
        long now = player.serverLevel().getGameTime();
        CHANNELS.put(player.getUUID(), new Channel(target.getUUID(), now, now + ProfessorXConfig.LINK_CHANNEL_TICKS.get()));
        HeroAbilities.setActive(player, LINK, true);
        SerumKit.meter(player, 0.0F);
        HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.4F);
        return 0.0D;
    }

    private static double cerebro(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        double radius = ProfessorXConfig.CEREBRO_RADIUS.get();
        int ticks = ProfessorXConfig.CEREBRO_GLOW_TICKS.get();
        List<LivingEntity> minds = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive() && !(entity instanceof ArmorStand) && !entity.isSpectator()
                        && entity.distanceToSqr(player) <= radius * radius);
        int dangerous = 0;
        for (LivingEntity mind : minds) {
            mind.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false), player);
            if (HeroAbilities.isBoss(mind)) {
                dangerous++;
            }
        }
        Vec3 feet = player.position().add(0.0D, 0.2D, 0.0D);
        if (!HeroFx.vfx(level, player.position(), "circle_ring", 0xFF000000 | MIND_COLOR, 4.0F)) {
            SerumKit.ring(level, feet, glow(player, 0.9F), 2.0D, 24);
        }
        SerumKit.ring(level, feet, glow(player, 0.8F), 4.0D, 40);
        SerumKit.ring(level, feet.add(0.0D, 0.8D, 0.0D), glow(player, 0.6F), 6.0D, 48);
        HeroFx.play(player, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.5F);
        HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.6F);
        String found = minds.isEmpty() ? "§7Cerebro finds no one out there."
                : "§dCerebro finds " + minds.size() + (minds.size() == 1 ? " mind." : " minds.");
        if (dangerous > 0) {
            found += dangerous == 1 ? " §cOne feels dangerous." : " §c" + dangerous + " feel dangerous.";
        }
        player.displayClientMessage(Component.literal(found), true);
        return ProfessorXConfig.CEREBRO_COOLDOWN.get();
    }

    private static double astral(ServerPlayer player, int slot) {
        if (AstralProjection.active(player)) {
            AstralProjection.end(player);
            player.displayClientMessage(Component.literal("§dYou settle back into your body."), true);
            return ProfessorXConfig.ASTRAL_COOLDOWN.get();
        }
        endChannel(player);
        AstralProjection.start(player, ProfessorXConfig.ASTRAL_TICKS.get());
        return 0.0D;
    }

    private static LivingEntity sighted(ServerPlayer player, double range) {
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
        if (beam.targets().isEmpty()) {
            return null;
        }
        LivingEntity target = beam.targets().get(0);
        return player.hasLineOfSight(target) ? target : null;
    }

    private static void tickChannel(ServerPlayer player) {
        Channel channel = CHANNELS.get(player.getUUID());
        if (channel == null) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        ServerLevel level = player.serverLevel();
        Entity entity = level.getEntity(channel.target());
        double range = ProfessorXConfig.LINK_RANGE.get() + 3.0D;
        if (!(entity instanceof Mob mob) || !mob.isAlive() || mob.distanceToSqr(player) > range * range
                || !player.hasLineOfSight(mob) || !PsychicLinks.canLink(player, mob)) {
            endChannel(player);
            player.displayClientMessage(Component.literal("§7You lost the connection."), true);
            HeroFx.play(player, SoundEvents.AMETHYST_BLOCK_BREAK, 0.6F, 1.0F);
            return;
        }
        float progress = (float) (now - channel.start()) / Math.max(1L, channel.end() - channel.start());
        SerumKit.meter(player, progress);
        if (now % 2L == 0L) {
            Vec3 from = player.getEyePosition().subtract(0.0D, 0.2D, 0.0D);
            Vec3 to = mob.getBoundingBox().getCenter();
            Vec3 reach = from.add(to.subtract(from).scale(Math.min(1.0F, progress + 0.1F)));
            HeroAbilities.particleLine(level, from, reach, glow(player, 0.6F), 0.5D);
        }
        if (now < channel.end()) {
            return;
        }
        endChannel(player);
        int ticks = ProfessorXConfig.LINK_TICKS.get();
        if (!PsychicLinks.link(player, mob, ticks, ProfessorXConfig.MAX_LINKS.get(), false)) {
            player.displayClientMessage(Component.literal("§7That mind won't open to you."), true);
            return;
        }
        HeroAbilities.setCooldown(player, LINK, ProfessorXConfig.LINK_COOLDOWN.get());
        Vec3 head = new Vec3(mob.getX(), mob.getEyeY() + 0.4D, mob.getZ());
        SerumKit.ring(level, head, glow(player, 0.9F), Math.max(0.4D, mob.getBbWidth() * 0.6D), 14);
        level.sendParticles(glow(player, 0.7F), head.x, head.y, head.z, 10, 0.25D, 0.25D, 0.25D, 0.0D);
        HeroFx.play(mob, SoundEvents.EVOKER_PREPARE_WOLOLO, 0.6F, 1.3F);
        player.displayClientMessage(Component.literal("§dLinked with " + mob.getName().getString() + " for "
                + Math.round(ticks / 20.0D) + " seconds."), true);
        if (PsychicLinks.count(player, false) >= 2) {
            HeroAdvancements.award(player, "x_men/professor_x/school_is_in_session");
        }
    }

    private static void endChannel(ServerPlayer player) {
        if (CHANNELS.remove(player.getUUID()) == null) {
            return;
        }
        HeroAbilities.setActive(player, LINK, false);
        SerumKit.meter(player, -1.0F);
        SerumKit.clearMeter(player);
    }

    private static void clear(ServerPlayer player) {
        endChannel(player);
        PsychicLinks.releaseAll(player);
        if (AstralProjection.active(player)) {
            AstralProjection.end(player);
        }
    }

    public static RogueMemory rogueMemory() {
        if (rogue == null) {
            rogue = RogueKit.memory("professor_x", "Professor X")
                    .active("Link", ProfessorXPower::rogueLink)
                    .passive(new RogueMemory.Passive() {
                        @Override
                        public void end(ServerPlayer player) {
                            PsychicLinks.releaseBorrowed(player);
                        }
                    })
                    .build();
        }
        return rogue;
    }

    private static double rogueLink(ServerPlayer player) {
        LivingEntity target = sighted(player, ProfessorXConfig.ROGUE_LINK_RANGE.get());
        if (!(target instanceof Mob mob) || !PsychicLinks.canLink(player, mob)
                || !PsychicLinks.link(player, mob, ProfessorXConfig.ROGUE_LINK_TICKS.get(), 1, true)) {
            player.displayClientMessage(Component.literal("§7Look at a hostile creature to reach its mind."), true);
            return 0.0D;
        }
        ServerLevel level = player.serverLevel();
        HeroAbilities.particleLine(level, player.getEyePosition().subtract(0.0D, 0.2D, 0.0D), mob.getBoundingBox().getCenter(),
                glow(player, 0.6F), 0.5D);
        HeroFx.play(mob, SoundEvents.EVOKER_PREPARE_WOLOLO, 0.6F, 1.3F);
        return ProfessorXConfig.ROGUE_LINK_COOLDOWN.get();
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CHANNELS.remove(player.getUUID());
            SerumKit.clearMeter(player);
        }
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endChannel(player);
        }
    }

    private static void onStopping(ServerStoppingEvent event) {
        CHANNELS.clear();
    }
}
