package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** Keeps the Reality Stone from making items: pasted mob echoes give nothing back, temporary rewrites can't be harvested. */
@EventBusSubscriber(modid = IntegratedSuperheroes.MODID)
public final class RealityEvents {
    private RealityEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()) RealityClipboard.onEchoJoin(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDrops(LivingDropsEvent event) {
        if (RealityClipboard.isEcho(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExperience(LivingExperienceDropEvent event) {
        if (RealityClipboard.isEcho(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onConvert(LivingConversionEvent.Pre event) {
        if (RealityClipboard.isEcho(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (RealityClipboard.isEcho(event.getTarget())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (RealityClipboard.isEcho(event.getTarget())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!GauntletGrief.probing() && RealityStone.dissolveTemporary(event.getLevel(), event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> RealityStone.dissolveTemporary(event.getLevel(), pos));
    }
}
