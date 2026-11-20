package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicVariants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class VariantCommand {
    private VariantCommand() {
    }

    static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> variant = Commands.literal("variant").requires(source -> source.hasPermission(2))
                .then(Commands.literal("pool").executes(VariantCommand::pool))
                .then(Commands.literal("tier").executes(VariantCommand::tier))
                .then(Commands.literal("clear").executes(VariantCommand::clear))
                .then(Commands.literal("audit").executes(VariantCommand::audit))
                .then(Commands.literal("nexus").executes(VariantCommand::nexus))
                .then(Commands.literal("spawn")
                        .executes(context -> spawnRandom(context, -1, true))
                        .then(Commands.literal("notear").executes(context -> spawnRandom(context, -1, false)))
                        .then(Commands.literal("random")
                                .executes(context -> spawnRandom(context, -1, true))
                                .then(Commands.literal("notear").executes(context -> spawnRandom(context, -1, false)))
                                .then(Commands.argument("tier", IntegerArgumentType.integer(0, VariantTiers.MAX))
                                        .executes(context -> spawnRandom(context, IntegerArgumentType.getInteger(context, "tier"), true))
                                        .then(Commands.literal("notear")
                                                .executes(context -> spawnRandom(context, IntegerArgumentType.getInteger(context, "tier"), false)))))
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(VariantKind.values()).map(VariantKind::key), builder))
                                .executes(context -> spawnKind(context, null, -1, true))
                                .then(Commands.literal("notear").executes(context -> spawnKind(context, null, -1, false)))
                                .then(Commands.argument("family", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            VariantKind kind = VariantKind.byKey(StringArgumentType.getString(context, "kind"));
                                            return kind == null ? builder.buildFuture() : SharedSuggestionProvider.suggest(
                                                    VariantPool.get(context.getSource().getServer()).families(kind).keySet(), builder);
                                        })
                                        .executes(context -> spawnKind(context, StringArgumentType.getString(context, "family"), -1, true))
                                        .then(Commands.literal("notear")
                                                .executes(context -> spawnKind(context, StringArgumentType.getString(context, "family"), -1, false)))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, VariantTiers.MAX))
                                                .executes(context -> spawnKind(context, StringArgumentType.getString(context, "family"),
                                                        IntegerArgumentType.getInteger(context, "level"), true))
                                                .then(Commands.literal("notear")
                                                        .executes(context -> spawnKind(context, StringArgumentType.getString(context, "family"),
                                                                IntegerArgumentType.getInteger(context, "level"), false)))))));
        dispatcher.register(Commands.literal("is").then(variant));
    }

    private static int nexus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        int tier = VariantTiers.forPlayer(player);
        if (!VariantTears.openNexus(level, player, tier)) {
            context.getSource().sendFailure(Component.literal("No room for a nexus near you"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Nexus opened at tier " + tier), true);
        return 1;
    }

    private static int pool(CommandContext<CommandSourceStack> context) {
        VariantPool.markDirty();
        VariantPool pool = VariantPool.get(context.getSource().getServer());
        List<String> lines = new ArrayList<>();
        for (VariantKind kind : VariantKind.values()) {
            int[] gates = pool.gateCounts(kind);
            int looks = 0;
            int ready = 0;
            for (VariantFamily family : pool.families(kind).values()) {
                looks += family.looks().size();
                if (!pool.readyLooks(family).isEmpty()) {
                    ready++;
                }
            }
            lines.add(kind.key() + ": " + pool.families(kind).size() + " families, " + looks + " looks, " + ready + " spawnable, "
                    + pool.villains(kind) + " villains; by gate " + Arrays.toString(gates) + "; budget " + pool.data().budgets.get(kind));
        }
        if (ModCompat.THEMATIC) {
            lines.addAll(ThematicVariants.poolLines(pool.families(VariantKind.THEMATIC)));
        }
        lines.forEach(line -> context.getSource().sendSuccess(() -> Component.literal(line), false));
        return 1;
    }

    private static int tier(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos = BlockPos.containing(context.getSource().getPosition());
        int tier = VariantTiers.at(level, pos);
        boolean spawns = VariantTiers.spawnsIn(level);
        int loaded = VoidVariants.loaded(level).size();
        context.getSource().sendSuccess(() -> Component.literal("Tier " + tier + ", spawning " + (spawns ? "on" : "off") + ", " + loaded + " loaded"), false);
        return tier;
    }

    private static int clear(CommandContext<CommandSourceStack> context) {
        List<Mob> loaded = VoidVariants.loaded(context.getSource().getLevel());
        loaded.forEach(Mob::discard);
        context.getSource().sendSuccess(() -> Component.literal("Removed " + loaded.size() + " Variants"), true);
        return loaded.size();
    }

    private static int audit(CommandContext<CommandSourceStack> context) {
        if (!ModCompat.THEMATIC) {
            context.getSource().sendFailure(Component.literal("The variant audit needs Thematic"));
            return 0;
        }
        VariantPool pool = VariantPool.get(context.getSource().getServer());
        List<String> lines = ThematicVariants.audit(context.getSource().getLevel(), context.getSource().getPosition(), pool.data().mobMoves.keySet());
        lines.forEach(line -> context.getSource().sendSuccess(() -> Component.literal(line), false));
        return lines.size();
    }

    private static int spawnRandom(CommandContext<CommandSourceStack> context, int tier, boolean tear) {
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos = spot(context);
        int used = tier < 0 ? VariantTiers.at(level, pos) : tier;
        VariantProfile profile = VariantPool.get(context.getSource().getServer()).roll(level.random, used, null);
        if (profile == null) {
            context.getSource().sendFailure(Component.literal("Could not spawn a Variant"));
            return 0;
        }
        return place(context, level, pos, profile, tear);
    }

    private static int place(CommandContext<CommandSourceStack> context, ServerLevel level, BlockPos pos, VariantProfile profile, boolean tear) {
        if (!tear) {
            return report(context, VoidVariants.spawnProfile(level, Vec3.atBottomCenterOf(pos), profile));
        }
        if (!VariantTears.arrive(level, Vec3.atBottomCenterOf(pos), profile, context.getSource().getPosition(), null)) {
            context.getSource().sendFailure(Component.literal("Could not open a tear there"));
            return 0;
        }
        context.getSource().sendSuccess(() -> VoidVariants.name(profile).copy().append(Component.literal(" " + profile.kind().key() + ":"
                + profile.family() + " look " + profile.look() + ", tier " + profile.tier() + ", " + profile.alignment().key() + ", arriving through a tear")), true);
        return 1;
    }

    private static int spawnKind(CommandContext<CommandSourceStack> context, String familyId, int tier, boolean tear) {
        VariantKind kind = VariantKind.byKey(StringArgumentType.getString(context, "kind"));
        if (kind == null) {
            context.getSource().sendFailure(Component.literal("Unknown kind"));
            return 0;
        }
        ServerLevel level = context.getSource().getLevel();
        BlockPos pos = spot(context);
        int used = tier < 0 ? VariantTiers.at(level, pos) : tier;
        VariantPool pool = VariantPool.get(context.getSource().getServer());
        VariantProfile profile;
        if (familyId == null) {
            profile = pool.roll(level.random, VariantTiers.MAX, kind);
            profile = profile == null ? null : new VariantProfile(profile.kind(), profile.family(), profile.look(), used, profile.alignment(), List.of());
        } else {
            VariantFamily family = pool.family(kind, familyId);
            if (family == null) {
                context.getSource().sendFailure(Component.literal("Unknown family " + familyId));
                return 0;
            }
            profile = pool.profile(family, null, used, level.random);
        }
        if (profile == null) {
            context.getSource().sendFailure(Component.literal("Nothing in that part of the pool can spawn yet"));
            return 0;
        }
        return place(context, level, pos, profile, tear);
    }

    private static BlockPos spot(CommandContext<CommandSourceStack> context) {
        Vec3 at = context.getSource().getPosition();
        Vec3 look = context.getSource().getRotation() == null ? Vec3.ZERO : Vec3.directionFromRotation(0.0F, context.getSource().getRotation().y);
        return BlockPos.containing(at.add(look.scale(4.0D)));
    }

    private static int report(CommandContext<CommandSourceStack> context, Mob mob) {
        if (mob == null) {
            context.getSource().sendFailure(Component.literal("Could not spawn a Variant"));
            return 0;
        }
        VariantProfile profile = VariantProfile.of(mob);
        String detail = profile == null ? "" : " " + profile.kind().key() + ":" + profile.family() + " look " + profile.look() + ", tier "
                + profile.tier() + ", " + profile.alignment().key() + ", moves " + profile.moves();
        context.getSource().sendSuccess(() -> mob.getDisplayName().copy().append(Component.literal(detail)), true);
        return 1;
    }
}
