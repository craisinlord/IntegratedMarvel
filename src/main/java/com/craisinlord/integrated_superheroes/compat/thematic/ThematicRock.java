package com.craisinlord.integrated_superheroes.compat.thematic;

import com.craisinlord.integrated_superheroes.hero.HeroContent;
import com.craisinlord.integrated_superheroes.villain.VillainConfig;
import com.craisinlord.integrated_superheroes.villain.VillainFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;

final class ThematicRock {
    private static final String THRONE = "thematic:shazam_throne";
    private static final String LODESTONE = "thematic:engraved_lodestone";

    private ThematicRock() {
    }

    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!VillainConfig.SPEC.isLoaded() || !VillainConfig.SHAZAM_THRONE.get()) {
            return;
        }
        Level world = event.getLevel();
        BlockPos pos = event.getPos();
        if (!THRONE.equals(id(world, pos)) || !LODESTONE.equals(id(world, pos.below()))) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(world instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Chosen chosen = Chosen.get(level);
        String key = player.getUUID() + "@" + pos.below().asLong();
        if (chosen.keys.contains(key)) {
            VillainFx.line(player, "integrated_superheroes.villains.shazam.silent", ChatFormatting.GRAY);
            return;
        }
        chosen.keys.add(key);
        chosen.setDirty();
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(pos));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 2.0F, 1.0F);
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D, new ItemStack(HeroContent.SHAZAM_SYRINGE.get()));
        item.setUnlimitedLifetime();
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        player.displayClientMessage(Component.translatable("integrated_superheroes.villains.shazam.chosen").withStyle(ChatFormatting.GOLD), false);
    }

    private static String id(Level level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    }

    static final class Chosen extends SavedData {
        private static final String NAME = "integrated_superheroes_rock_of_eternity";
        final Set<String> keys = new HashSet<>();

        static Chosen get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Chosen::new, Chosen::load, null), NAME);
        }

        private static Chosen load(CompoundTag tag, HolderLookup.Provider registries) {
            Chosen chosen = new Chosen();
            ListTag list = tag.getList("Chosen", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                chosen.keys.add(list.getString(i));
            }
            return chosen;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            keys.forEach(key -> list.add(StringTag.valueOf(key)));
            tag.put("Chosen", list);
            return tag;
        }
    }
}
