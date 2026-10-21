package com.craisinlord.integrated_superheroes.villain;

import com.craisinlord.integrated_superheroes.hero.HeroStun;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RedBook {
    private static final String KEY = "integrated_superheroes_red_book";
    private static final String[][] WORDS = {
            {"Желание", "Longing"},
            {"Ржавый", "Rusted"},
            {"Семнадцать", "Seventeen"},
            {"Рассвет", "Daybreak"},
            {"Печь", "Furnace"},
            {"Девять", "Nine"},
            {"Добросердечный", "Benign"},
            {"Возвращение на Родину", "Homecoming"},
            {"Один", "One"},
            {"Товарный вагон", "Freight car"}
    };
    private static final Map<UUID, Reading> READINGS = new HashMap<>();

    private RedBook() {
    }

    private static final class Reading {
        final UUID soldier;
        int word;
        int timer;

        Reading(UUID soldier) {
            this.soldier = soldier;
        }
    }

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough(Component.literal("ЗИМНИЙ СОЛДАТ\n\nWinter Soldier\n\n\n\nГИДРА\nHYDRA")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED))));
        StringBuilder russian = new StringBuilder();
        for (int i = 0; i < WORDS.length; i++) {
            russian.append(i + 1).append(". ").append(WORDS[i][0]).append('\n');
        }
        pages.add(Filterable.passThrough(Component.literal(russian.toString())));
        StringBuilder english = new StringBuilder();
        for (int i = 0; i < WORDS.length; i++) {
            english.append(i + 1).append(". ").append(WORDS[i][1]).append('\n');
        }
        pages.add(Filterable.passThrough(Component.literal(english.toString())));
        pages.add(Filterable.passThrough(Component.literal("Доброе утро, солдат.\n\nГотов отвечать.\n\n\nGood morning, soldier.\n\nReady to comply.")));
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Winter Soldier"), "HYDRA", 0, pages, true));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.integrated_superheroes.red_book").withStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.DARK_RED)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("item.integrated_superheroes.red_book.lore").withStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.GRAY)))));
        stack.set(DataComponents.RARITY, Rarity.EPIC);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(KEY, true));
        return stack;
    }

    public static boolean isBook(ItemStack stack) {
        return stack.is(Items.WRITTEN_BOOK) && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(KEY);
    }

    public static boolean holds(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (isBook(stack)) {
                return true;
            }
        }
        return isBook(player.getOffhandItem());
    }

    static boolean tryRead(ServerPlayer player) {
        WinterSoldierBrain soldier = lookedAt(player);
        if (soldier == null) {
            return false;
        }
        if (READINGS.containsKey(player.getUUID())) {
            return true;
        }
        if (!soldier.readable()) {
            VillainFx.line(player, "integrated_superheroes.villains.red_book.not_ready", ChatFormatting.GRAY);
            return true;
        }
        READINGS.put(player.getUUID(), new Reading(soldier.mob.getUUID()));
        VillainFx.line(player, "integrated_superheroes.villains.red_book.begin", ChatFormatting.DARK_RED);
        return true;
    }

    private static WinterSoldierBrain lookedAt(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        WinterSoldierBrain best = null;
        double bestDot = Math.cos(Math.toRadians(25.0D));
        for (VillainBrain brain : Villains.brains()) {
            if (!(brain instanceof WinterSoldierBrain soldier) || brain.mob.level() != player.level() || soldier.ally()) {
                continue;
            }
            Vec3 to = brain.mob.position().add(0.0D, brain.mob.getBbHeight() * 0.5D, 0.0D).subtract(eye);
            if (to.length() > 8.0D) {
                continue;
            }
            double dot = to.normalize().dot(look);
            if (dot >= bestDot) {
                bestDot = dot;
                best = soldier;
            }
        }
        return best;
    }

    static void onReaderHurt(ServerPlayer player) {
        Reading reading = READINGS.get(player.getUUID());
        if (reading != null && reading.word > 0) {
            reading.word = 0;
            reading.timer = 10;
            VillainFx.line(player, "integrated_superheroes.villains.red_book.interrupted", ChatFormatting.GRAY);
        }
    }

    static void tick(ServerLevel level) {
        if (READINGS.isEmpty()) {
            return;
        }
        List<UUID> done = new ArrayList<>();
        for (Map.Entry<UUID, Reading> entry : READINGS.entrySet()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                done.add(entry.getKey());
                continue;
            }
            if (player.level() != level) {
                continue;
            }
            Reading reading = entry.getValue();
            if (!(level.getEntity(reading.soldier) instanceof Mob soldier) || !soldier.isAlive()
                    || !(Villains.brain(soldier) instanceof WinterSoldierBrain brain) || brain.ally()
                    || soldier.distanceTo(player) > 10.0D || !holdsInHand(player)) {
                done.add(entry.getKey());
                VillainFx.line(player, "integrated_superheroes.villains.red_book.lost", ChatFormatting.GRAY);
                continue;
            }
            HeroStun.stun(soldier, 15);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 1, false, false));
            if (reading.timer-- > 0) {
                continue;
            }
            reading.timer = 10;
            if (reading.word >= WORDS.length) {
                done.add(entry.getKey());
                brain.becomeAlly(player);
                continue;
            }
            String[] word = WORDS[reading.word];
            Component text = Component.literal(word[1]).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                    .append(Component.literal("  " + word[0]).withStyle(ChatFormatting.GRAY));
            for (ServerPlayer near : VillainFx.near(level, soldier.position(), 24.0D)) {
                near.displayClientMessage(text, true);
            }
            VillainFx.sound(soldier, SoundEvents.NOTE_BLOCK_BASS.value(), 1.5F, 0.5F + reading.word * 0.08F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, soldier.getX(), soldier.getY(1.0D), soldier.getZ(),
                    6, 0.3D, 0.5D, 0.3D, 0.0D);
            reading.word++;
        }
        done.forEach(READINGS::remove);
    }

    private static boolean holdsInHand(ServerPlayer player) {
        return isBook(player.getMainHandItem()) || isBook(player.getOffhandItem());
    }
}
