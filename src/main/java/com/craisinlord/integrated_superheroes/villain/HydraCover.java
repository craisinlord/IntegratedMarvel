package com.craisinlord.integrated_superheroes.villain;

import com.craisinlord.integrated_superheroes.hero.HeroStun;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tintankgames.marvel.world.entity.HydraAgent;
import net.tintankgames.marvel.world.entity.RedSkull;

import java.util.Locale;

public final class HydraCover {
    private static final String KEY_UNTIL = "integrated_superheroes_hydra_cover_until";
    private static final String KEY_READY = "integrated_superheroes_hydra_cover_ready";

    private HydraCover() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(HydraCover::onChat);
        NeoForge.EVENT_BUS.addListener(HydraCover::onPlayerTick);
    }

    public static boolean covered(Entity entity) {
        return entity instanceof Player player && VillainConfig.HAIL_HYDRA_ENABLED.get()
                && player.getPersistentData().getLong(KEY_UNTIL) > player.level().getGameTime();
    }

    public static void blow(Player player) {
        if (!covered(player) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        serverPlayer.getPersistentData().remove(KEY_UNTIL);
        VillainFx.line(serverPlayer, "integrated_superheroes.villains.hydra.cover.blown", ChatFormatting.RED);
    }

    private static void onChat(ServerChatEvent event) {
        if (!VillainConfig.enabled() || !VillainConfig.HAIL_HYDRA_ENABLED.get() || !saysHail(event.getRawText())) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        player.server.execute(() -> hail(player));
    }

    static boolean saysHail(String text) {
        String words = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z]+", " ").trim();
        return (" " + words + " ").contains(" hail hydra ");
    }

    private static void hail(ServerPlayer player) {
        if (!player.isAlive() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        CompoundTag data = player.getPersistentData();
        if (data.getLong(KEY_READY) > now) {
            VillainFx.line(player, "integrated_superheroes.villains.hydra.cover.cooldown", ChatFormatting.GRAY);
            return;
        }
        data.putLong(KEY_UNTIL, now + VillainConfig.HAIL_HYDRA_SECONDS.get() * 20L);
        data.putLong(KEY_READY, now + VillainConfig.HAIL_HYDRA_COOLDOWN_SECONDS.get() * 20L);
        double radius = VillainConfig.ALERT_RADIUS.get();
        AABB area = player.getBoundingBox().inflate(radius);
        boolean answered = false;
        for (HydraAgent agent : level.getEntitiesOfClass(HydraAgent.class, area, LivingEntity::isAlive)) {
            if (agent.getTarget() == player) {
                agent.setTarget(null);
            }
            HeroStun.stun(agent, 20);
            agent.getLookControl().setLookAt(player);
            answered = true;
        }
        if (answered) {
            VillainFx.line(level, player.position(), radius + 8.0D, "integrated_superheroes.villains.hydra.hail", ChatFormatting.DARK_RED);
            Villains.award(player, "avengers/hail_hydra");
        }
        if (!level.getEntitiesOfClass(RedSkull.class, area, LivingEntity::isAlive).isEmpty()) {
            VillainFx.line(player, "integrated_superheroes.villains.hydra.cover.red_skull", ChatFormatting.DARK_RED);
        } else {
            VillainFx.line(player, answered ? "integrated_superheroes.villains.hydra.cover.answered" : "integrated_superheroes.villains.hydra.cover.start",
                    ChatFormatting.DARK_RED);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        if (data.contains(KEY_UNTIL) && data.getLong(KEY_UNTIL) <= player.level().getGameTime()) {
            data.remove(KEY_UNTIL);
            VillainFx.line(player, "integrated_superheroes.villains.hydra.cover.end", ChatFormatting.GRAY);
        }
    }
}
