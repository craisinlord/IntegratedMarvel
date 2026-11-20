package com.craisinlord.integrated_superheroes.portal;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.thematic.SuitPieces;
import com.craisinlord.integrated_superheroes.gauntlet.SpacePortalManager;
import com.craisinlord.integrated_superheroes.gauntlet.SpaceTravel;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record EldritchPortalCast() implements CustomPacketPayload {
    private static final ResourceLocation SUIT = ResourceLocation.fromNamespaceAndPath("thematic", "dr_strange");
    private static final String COOLDOWN = "integrated_superheroes:eldritch_portal_ready";
    public static final EldritchPortalCast INSTANCE = new EldritchPortalCast();
    public static final Type<EldritchPortalCast> TYPE = new Type<>(IntegratedSuperheroes.id("eldritch_portal"));

    @Override
    public Type<EldritchPortalCast> type() {
        return TYPE;
    }

    static boolean canCast(Player player) {
        Item chest = player.getItemBySlot(EquipmentSlot.CHEST).getItem();
        return ModCompat.THEMATIC && MarvelPortal.enabled() && player.isAlive() && !player.isSpectator()
                && !player.isPassenger() && SUIT.equals(BuiltInRegistries.ITEM.getKey(chest))
                && SuitPieces.complete(player, chest);
    }

    private static void cast(ServerPlayer player) {
        if (!canCast(player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = player.getServer().overworld().getGameTime();
        if (now < player.getPersistentData().getLong(COOLDOWN)
                || !(player.pick(32, 1, false) instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        player.getPersistentData().putLong(COOLDOWN, now + 40);
        BlockPos feet = hit.getBlockPos().above();
        if (level.hasChunkAt(feet) && level.getWorldBorder().isWithinBounds(feet)
                && SpaceTravel.isStandable(level, feet) && feet.distSqr(player.blockPosition()) >= 25) {
            SpacePortalManager.createEldritch(level, player.position().add(0, 0.9, 0),
                    player.getLookAngle(), feet, player);
        }
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE, StreamCodec.<ByteBuf, EldritchPortalCast>unit(INSTANCE),
                    (payload, context) -> context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            cast(player);
                        }
                    }));
        }
    }
}
