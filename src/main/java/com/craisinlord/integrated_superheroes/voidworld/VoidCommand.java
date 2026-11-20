package com.craisinlord.integrated_superheroes.voidworld;

import com.mojang.brigadier.CommandDispatcher;
import com.craisinlord.integrated_superheroes.boss.WitherstormEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class VoidCommand {
    private VoidCommand() {
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("integrated_superheroes").requires(source -> source.hasPermission(2))
                .then(Commands.literal("void")
                        .then(Commands.literal("tier").executes(VoidCommand::tier))
                        .then(Commands.literal("send")
                                .executes(context -> send(context, List.of(context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> send(context, EntityArgument.getPlayers(context, "targets")))))
                        .then(Commands.literal("storm")
                                .executes(VoidCommand::stormInfo)
                                .then(Commands.literal("spawn")
                                        .executes(context -> stormSpawn(context, 0))
                                        .then(Commands.argument("stage", IntegerArgumentType.integer(2, 5))
                                                .executes(context -> stormSpawn(context, IntegerArgumentType.getInteger(context, "stage")))))
                                .then(Commands.literal("remove").executes(VoidCommand::stormRemove))
                                .then(Commands.literal("stage")
                                        .then(Commands.argument("stage", IntegerArgumentType.integer(2, 5))
                                                .executes(context -> stormStage(context, IntegerArgumentType.getInteger(context, "stage"))))))
                        .then(Commands.literal("pool").executes(VoidCommand::pool))
                        .then(Commands.literal("vehicle")
                                .executes(context -> vehicle(context, false))
                                .then(Commands.literal("tear").executes(context -> vehicle(context, true))))
                        .then(Commands.literal("prune")
                                .executes(context -> prune(context, false))
                                .then(Commands.literal("here").executes(context -> prune(context, true))))
                        .then(Commands.literal("debris")
                                .executes(context -> debris(context, PrunedDebrisPool.DEFAULT_GROUP))
                                .then(Commands.argument("group", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(PrunedDebrisPool.groups().keySet(), builder))
                                        .executes(context -> debris(context, StringArgumentType.getString(context, "group")))))));
    }

    private static int tier(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int distance = VoidTier.tierAt(player.getBlockX(), player.getBlockZ());
        int yours = VoidTier.forPlayer(player);
        long minutes = VoidTier.visitTicks(player) / 1200L;
        context.getSource().sendSuccess(() -> Component.literal("Ground tier " + distance + ", your tier " + yours
                + (VoidWorld.isVoid(player.level()) ? ", " + minutes + " min this visit" : ", not in the Void")), false);
        return yours;
    }

    private static int send(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        int sent = 0;
        for (ServerPlayer player : players) {
            if (VoidTravel.send(player)) {
                sent++;
            }
        }
        int count = sent;
        if (count == 0) {
            context.getSource().sendFailure(Component.literal("The Void is not loaded"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Sent " + count + " to the Void"), true);
        return count;
    }

    private static ServerLevel voidLevel(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getServer().getLevel(VoidWorld.LEVEL);
        if (level == null) context.getSource().sendFailure(Component.literal("The Void is not loaded"));
        return level;
    }

    private static int stormInfo(CommandContext<CommandSourceStack> context) {
        ServerLevel level = voidLevel(context);
        if (level == null) return 0;
        WitherstormEntity storm = VoidStorm.current(level);
        if (storm == null) {
            long minutes = VoidStorm.minutesUntilSpawn(level);
            context.getSource().sendSuccess(() -> Component.literal(minutes > 0 ? "No storm, next one in " + minutes + " min" : "No storm right now"), false);
            return 0;
        }
        BlockPos pos = storm.blockPosition();
        context.getSource().sendSuccess(() -> Component.literal("Storm stage " + storm.getStage() + " at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + ", " + Math.round(storm.getHealth()) + "/" + Math.round(storm.getMaxHealth()) + " health"), false);
        return storm.getStage();
    }

    private static int stormSpawn(CommandContext<CommandSourceStack> context, int stage) {
        ServerLevel level = voidLevel(context);
        if (level == null) return 0;
        ServerPlayer near = context.getSource().getPlayer();
        if (near != null && near.level() != level) near = null;
        WitherstormEntity storm = VoidStorm.forceSpawn(level, near, stage);
        if (storm == null) {
            context.getSource().sendFailure(Component.literal("Couldn't place the storm. Someone has to be in the Void and the spot has to be loaded"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Storm spawned at stage " + storm.getStage()), true);
        return 1;
    }

    private static int stormRemove(CommandContext<CommandSourceStack> context) {
        ServerLevel level = voidLevel(context);
        if (level == null) return 0;
        boolean had = VoidStorm.remove(level);
        long minutes = VoidStorm.minutesUntilSpawn(level);
        context.getSource().sendSuccess(() -> Component.literal((had ? "Storm removed" : "No storm to remove")
                + ", it comes back in " + minutes + " min unless you spawn one"), true);
        return had ? 1 : 0;
    }

    private static int stormStage(CommandContext<CommandSourceStack> context, int stage) {
        ServerLevel level = voidLevel(context);
        if (level == null) return 0;
        boolean live = VoidStorm.setStage(level, stage);
        context.getSource().sendSuccess(() -> Component.literal(live ? "Storm is now stage " + stage : "No storm right now, the next one starts at stage " + stage), true);
        return 1;
    }

    private static int prune(CommandContext<CommandSourceStack> context, boolean here) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!VoidWorld.isVoid(player.level())) {
            context.getSource().sendFailure(Component.literal("You have to be in the Void"));
            return 0;
        }
        com.craisinlord.integrated_superheroes.voidworld.pruning.PrunedPiece piece =
                com.craisinlord.integrated_superheroes.voidworld.pruning.VoidPruning.force(player.serverLevel(), player, here);
        if (piece == null) {
            context.getSource().sendFailure(Component.literal("Nothing pruned (no pick, a rejected pick, or no ground nearby)"));
            return 0;
        }
        BlockPos at = piece.centre();
        context.getSource().sendSuccess(() -> Component.literal("Pruning " + piece.id() + " onto " + at.getX() + " " + at.getY() + " " + at.getZ()), false);
        return 1;
    }

    private static int vehicle(CommandContext<CommandSourceStack> context, boolean tear) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!com.craisinlord.integrated_superheroes.compat.ModCompat.MTS) {
            context.getSource().sendFailure(Component.literal("Immersive Vehicles isn't installed"));
            return 0;
        }
        if (!VoidWorld.isVoid(player.level())) {
            context.getSource().sendFailure(Component.literal("You have to be in the Void"));
            return 0;
        }
        com.craisinlord.integrated_superheroes.voidworld.vehicles.VoidVehicles.Result result =
                com.craisinlord.integrated_superheroes.voidworld.vehicles.VoidVehicles.force(player.serverLevel(), player, tear);
        if (result == null) {
            context.getSource().sendFailure(Component.literal("No vehicle placed (no flat ground nearby, or no vehicle could be fitted out)"));
            return 0;
        }
        BlockPos at = BlockPos.containing(result.at());
        context.getSource().sendSuccess(() -> Component.literal((result.tear() ? "Opening " : "Placed ") + result.what() + " at " + at.getX() + " " + at.getY() + " " + at.getZ()), false);
        return 1;
    }

    private static int pool(CommandContext<CommandSourceStack> context) {
        Map<String, List<PrunedDebrisPool.Entry>> groups = PrunedDebrisPool.groups();
        if (groups.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Debris pool is empty"));
            return 0;
        }
        groups.forEach((group, entries) -> {
            int templates = entries.stream().mapToInt(entry -> entry.ids().size()).sum();
            context.getSource().sendSuccess(() -> Component.literal(group + ": " + entries.size() + " files, " + templates + " templates"), false);
            for (PrunedDebrisPool.Entry entry : entries) {
                context.getSource().sendSuccess(() -> Component.literal("  " + entry.source() + " w" + entry.weight()
                        + " t" + entry.minTier() + "-" + entry.maxTier() + ": " + entry.ids().size()), false);
            }
        });
        return 1;
    }

    private static int debris(CommandContext<CommandSourceStack> context, String group) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerLevel level = player.serverLevel();
        Optional<Holder.Reference<StructureProcessorList>> processors = level.registryAccess()
                .registryOrThrow(Registries.PROCESSOR_LIST).getHolder(VoidWorld.PRUNED_PROCESSORS);
        if (processors.isEmpty()) {
            context.getSource().sendFailure(Component.literal("Missing processor list " + VoidWorld.PRUNED_PROCESSORS.location()));
            return 0;
        }
        BlockPos pos = player.blockPosition();
        int tier = VoidTier.tierAt(pos.getX(), pos.getZ());
        ResourceLocation placed = PrunedDebrisFeature.placeAt(level, pos, group, tier, level.getRandom(), processors.get(), 0.25F, 0.6F, null);
        if (placed == null) {
            context.getSource().sendFailure(Component.literal("Nothing placed (no pick, a rejected pick, or no ground)"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("Placed " + placed), false);
        return 1;
    }
}
